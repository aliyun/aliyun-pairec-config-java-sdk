package com.aliyun.openservices.pairec.recallengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * RecallEngine Client.
 * <ul>
 *   <li>recall: synchronous</li>
 *   <li>write: synchronous, buffered and batched</li>
 * </ul>
 *
 * <p>Rows handed to {@link #write} are buffered in memory. Once the buffer
 * reaches {@code batchSize} the HTTP request is issued <em>on the calling
 * thread</em>. The caller is therefore paced by the actual write throughput of
 * the backend, which propagates backpressure to the producer (e.g. a Flink
 * sink) and keeps the buffer from growing without bound.
 */
public class RecallEngineClient {
    public static final Logger logger = LoggerFactory.getLogger(RecallEngineClient.class);

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final int DEFAULT_TIMEOUT_MS = 500;
    // JVM-wide counter so flush timer thread names stay unique across multiple
    // RecallEngineClient instances (e.g. Flink parallel sub-tasks sharing one
    // TaskManager).
    private static final AtomicInteger FLUSH_TIMER_THREAD_COUNTER = new AtomicInteger();

    private String endpoint;
    private String username;
    private String password;
    int retryTimes;
    Map<String, String> requestHeaders;
    private OkHttpClient httpClient;
    private String authCache;
    private ObjectMapper objectMapper;

    // --- Write buffer ---
    private final List<WriteItem> writeData = new ArrayList<>();
    private final ReentrantLock writeLock = new ReentrantLock();
    private final Condition writeCondition = writeLock.newCondition();
    private volatile boolean running = true;
    private volatile Thread flushTimerThread;

    // Flush when the buffer reaches this many rows; also the row cap of a
    // single HTTP request, so a traffic burst cannot be coalesced into one
    // oversized body.
    private int batchSize = 200;
    // Flush whatever is buffered after this long, so low-traffic streams do not
    // sit in the buffer waiting for a full batch.
    private long flushIntervalMs = 50;

    /**
     * Create a new RecallEngineClient
     */
    public RecallEngineClient(String endpoint, String username, String password) {
        this.endpoint = endpoint;
        this.username = username;
        this.password = password;
        this.retryTimes = 0;
        this.requestHeaders = new HashMap<>();
        this.objectMapper = new ObjectMapper();

        // Ensure endpoint has schema
        if (!this.endpoint.startsWith("http://") && !this.endpoint.startsWith("https://")) {
            this.endpoint = "http://" + this.endpoint;
        }

        // Create default HTTP client
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(200, TimeUnit.MILLISECONDS)
                .readTimeout(DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .writeTimeout(DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .connectionPool(new ConnectionPool(1000, 5, TimeUnit.MINUTES))
                .build();
    }

    // ==================== Configuration Methods ====================

    public RecallEngineClient withRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
        return this;
    }

    public RecallEngineClient withRequestHeader(String key, String value) {
        this.requestHeaders.put(key, value);
        return this;
    }

    public RecallEngineClient withHttpClient(OkHttpClient httpClient) {
        this.httpClient = httpClient;
        return this;
    }

    /**
     * Configure the write batch size: the buffer is flushed as soon as it holds
     * this many rows, and no single HTTP request carries more than this many
     * rows.
     *
     * @param batchSize rows per batch (default: 200)
     * @return this client for method chaining
     */
    public RecallEngineClient withBatchSize(int batchSize) {
        if (batchSize <= 0) throw new IllegalArgumentException("Batch size must be positive");
        this.batchSize = batchSize;
        return this;
    }

    /**
     * Configure how long buffered rows may wait before being flushed even
     * though the buffer has not reached {@code batchSize}.
     *
     * @param flushIntervalMs flush interval in milliseconds (default: 50)
     * @return this client for method chaining
     */
    public RecallEngineClient withFlushInterval(long flushIntervalMs) {
        if (flushIntervalMs <= 0) throw new IllegalArgumentException("Flush interval must be positive");
        this.flushIntervalMs = flushIntervalMs;
        return this;
    }

    // ==================== Recall (Synchronous) ====================

    public RecallResponse recall(RecallRequest request) throws RecallEngineException {
        if (retryTimes > 0) {
            RecallEngineException lastException = null;
            for (int i = 0; i < retryTimes; i++) {
                try {
                    return doRecall(request);
                } catch (RecallEngineException e) {
                    lastException = e;
                    logger.warn("recallengine: recall failed, retrying..., err: {}", e.getMessage());
                }
            }
            throw lastException;
        } else {
            return doRecall(request);
        }
    }

    private RecallResponse doRecall(RecallRequest request) throws RecallEngineException {
        try {
            String json = objectMapper.writeValueAsString(request);
            String url = endpoint + "/api/v1/recall";
            RequestBody body = RequestBody.create(json, JSON);

            Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .post(body)
                    .header("Content-Type", "application/json")
                    .header("Auth", buildAuth());

            for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
                requestBuilder.header(entry.getKey(), entry.getValue());
            }

            try (okhttp3.Response response = httpClient.newCall(requestBuilder.build()).execute()) {
                if (response.body() == null) {
                    throw new RecallEngineException("Empty response body");
                }

                byte[] responseBytes = response.body().bytes();

                if (response.code() != 200) {
                    String errorMsg = "response status code: " + response.code();
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> errorMap = objectMapper.readValue(responseBytes, Map.class);
                        if (errorMap.containsKey("message")) {
                            errorMsg = "response status code: " + response.code() + ", message: " + errorMap.get("message");
                        }
                    } catch (Exception e) {
                        logger.debug("Failed to parse error response", e);
                    }
                    throw new RecallEngineException(errorMsg, response.code());
                }

                Record record = RecordUtils.unserializeRecord(responseBytes);
                return new RecallResponse(record);
            }
        } catch (RecallEngineException e) {
            throw e;
        } catch (Exception e) {
            throw new RecallEngineException("Recall request failed", e);
        }
    }

    // ==================== Write (Synchronous, Buffered) ====================

    /**
     * Buffer rows for writing and, once a full batch has accumulated, send it
     * synchronously on the calling thread.
     *
     * <p>The returned {@link WriteResponse} acknowledges buffering, not
     * server-side persistence: a batch that fails every retry is logged and
     * dropped so that the producer keeps running.
     */
    public WriteResponse write(String instanceId, String table, WriteRequest request) {
        // Fail fast if the client has already been closed. Without this check,
        // data would be added to the buffer after close() drained it, with
        // nothing left to flush it (silent data loss).
        if (!running) {
            throw new IllegalStateException(
                    "RecallEngineClient is closed; cannot accept new writes");
        }

        startFlushTimer();

        if (request == null || request.getContent() == null || request.getContent().isEmpty()) {
            WriteResponse response = new WriteResponse();
            response.setRequestId(request != null ? request.getRequestId() : null);
            response.setCode("OK");
            response.setMessage("Empty request");
            return response;
        }

        int itemCount = request.getContent().size();
        InsertMode insertMode = request.getInsertMode();

        writeLock.lock();
        try {
            for (Map<String, Object> data : request.getContent()) {
                writeData.add(new WriteItem(instanceId, table, data, insertMode));
            }
            if (writeData.size() >= batchSize) {
                // Write on the caller's thread. The caller is thereby paced by
                // the backend, backpressure reaches the producer, and the
                // buffer cannot outgrow one batch plus one call's worth of rows.
                flushBufferLocked();
            }
        } finally {
            writeLock.unlock();
        }

        WriteResponse response = new WriteResponse();
        response.setRequestId(request.getRequestId());
        response.setCode("OK");
        response.setMessage(String.format("Buffered %d items for write", itemCount));
        return response;
    }

    /**
     * Flush every buffered row synchronously.
     */
    public void writeFlush() {
        writeLock.lock();
        try {
            flushBufferLocked();
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Write out the whole buffer, then clear it.
     *
     * <p><b>MUST be called while holding {@code writeLock}.</b> Holding the
     * lock across the HTTP call is intentional: concurrent producers block
     * until the in-flight batch completes, which is exactly the backpressure
     * this client is meant to apply.
     *
     * <p>Rows are grouped by instance/table/insert-mode (one request per
     * group) and each group is split into chunks of at most {@code batchSize}
     * rows. A chunk that fails every retry is logged and dropped; the buffer is
     * always cleared so a failure cannot make the buffer grow or resend rows.
     */
    private void flushBufferLocked() {
        if (writeData.isEmpty()) {
            return;
        }
        try {
            // LinkedHashMap: keep the order rows arrived in, so writes to a
            // given table stay in submission order.
            Map<String, List<WriteItem>> grouped = new LinkedHashMap<>();
            for (WriteItem item : writeData) {
                String key = item.instanceId + "|" + item.table + "|" + item.insertMode.getValue();
                grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
            }

            for (List<WriteItem> items : grouped.values()) {
                String instanceId = items.get(0).instanceId;
                String table = items.get(0).table;
                InsertMode insertMode = items.get(0).insertMode;

                for (int start = 0; start < items.size(); start += batchSize) {
                    int end = Math.min(start + batchSize, items.size());
                    List<Map<String, Object>> content = new ArrayList<>(end - start);
                    for (WriteItem item : items.subList(start, end)) {
                        content.add(item.data);
                    }

                    WriteRequest request = new WriteRequest();
                    request.setContent(content);
                    request.setInsertMode(insertMode);

                    try {
                        writeWithRetry(instanceId, table, request);
                        logger.debug("write completed: {} items to {}/{}", content.size(), instanceId, table);
                    } catch (Exception e) {
                        // Keep the producer running: report the loss and move on
                        // to the next chunk rather than failing the whole flush.
                        logger.error("write failed for {}/{}, dropping {} items: {}",
                                instanceId, table, content.size(), e.getMessage(), e);
                    }
                }
            }
        } finally {
            // Never leave already-processed rows in the buffer, whatever happened.
            writeData.clear();
        }
    }

    /**
     * Issue one write request, retrying transient failures.
     *
     * <p>{@code retryTimes} is the total number of attempts and is floored at
     * 1, so a client left at the default still sends the request once instead
     * of silently discarding the batch. Only throttling, server errors and
     * transport failures are retried; a 4xx will not succeed on a second try.
     */
    private void writeWithRetry(String instanceId, String table, WriteRequest request)
            throws RecallEngineException {
        int maxAttempts = Math.max(1, retryTimes);
        for (int attempt = 1; ; attempt++) {
            try {
                doWrite(instanceId, table, request);
                return;
            } catch (RecallEngineException e) {
                if (attempt >= maxAttempts || !isRetryable(e.getStatusCode())) {
                    throw e;
                }
                logger.warn("write failed for {}/{}, retrying ({}/{}), err: {}",
                        instanceId, table, attempt, maxAttempts, e.getMessage());
            }
        }
    }

    /**
     * Throttling and server-side errors are worth another attempt, as is a
     * failure that never produced a response (connect/read timeout). Every
     * other 4xx is a problem with the request itself and will fail again.
     */
    private static boolean isRetryable(int statusCode) {
        return statusCode == RecallEngineException.NO_STATUS_CODE
                || statusCode == 429
                || statusCode >= 500;
    }

    /**
     * Perform a single write HTTP call. Package-visible rather than private so
     * tests can substitute failures and observe the batching and retry
     * behaviour without a server.
     */
    WriteResponse doWrite(String instanceId, String table, WriteRequest request) throws RecallEngineException {
        try {
            String json = objectMapper.writeValueAsString(request);
            String url = String.format("%s/api/v1/tables/%s/default/%s/write", endpoint, instanceId, table);
            RequestBody body = RequestBody.create(json, JSON);

            Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .post(body)
                    .header("Content-Type", "application/json")
                    .header("Auth", buildAuth());

            for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
                requestBuilder.header(entry.getKey(), entry.getValue());
            }

            try (okhttp3.Response response = httpClient.newCall(requestBuilder.build()).execute()) {
                if (response.body() == null) {
                    throw new RecallEngineException("Empty response body");
                }

                String responseBody = response.body().string();
                if (response.code() != 200) {
                    String errorMsg = "write request failed, status: " + response.code();
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> errorMap = objectMapper.readValue(responseBody, Map.class);
                        if (errorMap.containsKey("message")) {
                            errorMsg += ", message: " + errorMap.get("message");
                        }
                    } catch (Exception e) {
                        // Ignore parse error
                    }
                    throw new RecallEngineException(errorMsg, response.code());
                }

                return objectMapper.readValue(responseBody, WriteResponse.class);
            }
        } catch (RecallEngineException e) {
            throw e;
        } catch (Exception e) {
            // No status code: transport or serialization failure, treated as retryable.
            throw new RecallEngineException("Write request failed", e);
        }
    }

    private String buildAuth() {
        if (authCache == null) {
            String credentials = username + ":" + password;
            authCache = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        }
        return authCache;
    }

    /**
     * Start the flush timer thread on first write.
     *
     * <p>The thread performs no writing of its own beyond bounding staleness:
     * it flushes a partial buffer once {@code flushIntervalMs} has elapsed, so
     * a low-traffic stream does not have to wait for a full batch. Writes it
     * triggers are synchronous, under the same lock as {@link #write}.
     *
     * <p>Double-checked locking keeps the common case lock-free.
     */
    private void startFlushTimer() {
        Thread thread = flushTimerThread;
        if (thread != null && thread.isAlive()) {
            return;
        }

        synchronized (this) {
            thread = flushTimerThread;
            if (thread != null && thread.isAlive()) {
                return;
            }

            String threadName = "RecallEngineFlushTimer-" + FLUSH_TIMER_THREAD_COUNTER.incrementAndGet();
            flushTimerThread = new Thread(() -> {
                while (running) {
                    writeLock.lock();
                    try {
                        writeCondition.await(flushIntervalMs, TimeUnit.MILLISECONDS);
                        flushBufferLocked();
                    } catch (InterruptedException e) {
                        logger.warn("{} interrupted, flushing remaining data before exit", threadName);
                        // Flush before re-asserting the interrupt: the whole
                        // point of this flush is to save the last rows, and an
                        // already-interrupted thread risks having its HTTP call
                        // aborted underneath it.
                        flushBufferLocked();
                        Thread.currentThread().interrupt();
                        break;
                    } finally {
                        writeLock.unlock();
                    }
                }
                logger.info("{} has stopped.", threadName);
            }, threadName);
            flushTimerThread.setDaemon(true);
            flushTimerThread.start();
        }
    }

    /**
     * Flush whatever is buffered and stop accepting writes.
     */
    public void close() {
        this.running = false;

        writeLock.lock();
        try {
            writeCondition.signalAll();
        } finally {
            writeLock.unlock();
        }

        // The timer thread observes running=false within at most
        // flushIntervalMs and flushes on its way out; no hard timeout here,
        // because cutting it off mid-flush would drop buffered rows.
        Thread thread = flushTimerThread;
        if (thread != null && thread.isAlive()) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Anything the timer thread did not get to.
        writeFlush();
    }

    // ==================== Inner Class ====================

    private static class WriteItem {
        final String instanceId;
        final String table;
        final Map<String, Object> data;
        final InsertMode insertMode;

        WriteItem(String instanceId, String table, Map<String, Object> data, InsertMode insertMode) {
            this.instanceId = instanceId;
            this.table = table;
            this.data = data;
            this.insertMode = insertMode;
        }
    }
}
