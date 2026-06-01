package com.aliyun.openservices.pairec.recallengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * RecallEngine Client.
 * - recall: Synchronous
 * - write: Asynchronous (Buffered & Batched)
 */
public class RecallEngineClient {
    public static final Logger logger = LoggerFactory.getLogger(RecallEngineClient.class);

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final int DEFAULT_TIMEOUT_MS = 500;

    // --- Original Fields ---
    private String endpoint;
    private String username;
    private String password;
    int retryTimes;
    Map<String, String> requestHeaders;
    private OkHttpClient httpClient;
    private String authCache;
    private ObjectMapper objectMapper;

    // --- Async Write Infrastructure ---
    private final List<WriteItem> writeData = new ArrayList<>();
    private final ReentrantLock writeLock = new ReentrantLock();
    private final Condition writeCondition = writeLock.newCondition();
    private volatile ExecutorService writeExecutor;
    private volatile boolean running = true;
    private volatile Thread asyncWriteThread;

    // Async Configs (Defaults)
    private int batchSize = 20;       // Flush when buffer reaches this size
    private long flushIntervalMs = 50; // Or every 50ms
    private long flushTimeoutMs = 10000; // Max time writeFlush() waits for an in-flight HTTP batch
    private int writeThreadPoolSize = 4;

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

    // Async Write Configs
    public RecallEngineClient withBatchSize(int batchSize) {
        if (batchSize <= 0) throw new IllegalArgumentException("Batch size must be positive");
        this.batchSize = batchSize;
        return this;
    }

    public RecallEngineClient withFlushInterval(long flushIntervalMs) {
        if (flushIntervalMs <= 0) throw new IllegalArgumentException("Flush interval must be positive");
        this.flushIntervalMs = flushIntervalMs;
        return this;
    }

    /**
     * Configure the maximum time {@link #writeFlush()} will wait for the
     * pending HTTP write batch to complete before giving up.
     * <p>
     * The flush HTTP request is executed outside the buffer lock; this timeout
     * only bounds how long callers (e.g. {@link #close()} or a Flink Sink
     * checkpoint) are willing to wait. On timeout the in-flight future is
     * cancelled and the call returns; data already submitted to the executor
     * may still be sent best-effort.
     *
     * @param flushTimeoutMs flush timeout in milliseconds (default: 10000)
     * @return this client for method chaining
     */
    public RecallEngineClient withFlushTimeoutMs(long flushTimeoutMs) {
        if (flushTimeoutMs <= 0) throw new IllegalArgumentException("Flush timeout must be positive");
        this.flushTimeoutMs = flushTimeoutMs;
        return this;
    }

    public RecallEngineClient withWriteThreadPoolSize(int poolSize) {
        if (poolSize <= 0) throw new IllegalArgumentException("Thread pool size must be positive");
        this.writeThreadPoolSize = poolSize;
        return this;
    }

    // ==================== Recall (Synchronous - Unchanged) ====================

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
                    throw new RecallEngineException(errorMsg);
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

    // ==================== Write (Asynchronous) ====================

    /**
     * Make an ASYNC write request.
     * Data is buffered and sent in batches by a background thread.
     * This method returns immediately.
     *
     * Signature matches the original synchronous version.
     */
    public WriteResponse write(String instanceId, String table, WriteRequest request) {
        // Fail fast if the client has already been closed. Without this check,
        // data would be added to the buffer after close() drained it, with no
        // background thread or executor to consume it (silent data loss).
        if (!running) {
            throw new IllegalStateException(
                    "RecallEngineClient is closed; cannot accept new writes");
        }

        // 1. Start background thread if needed
        startAsyncWriteThread();

        // 2. Handle empty request
        if (request == null || request.getContent() == null || request.getContent().isEmpty()) {
            WriteResponse response = new WriteResponse();
            response.setRequestId(request != null ? request.getRequestId() : null);
            response.setCode("OK");
            response.setMessage("Empty request");
            return response;
        }

        int itemCount = request.getContent().size();

        // 3. Add to buffer
        writeLock.lock();
        try {
            for (Map<String, Object> data : request.getContent()) {
                writeData.add(new WriteItem(instanceId, table, data));
            }
            // Signal if batch size reached
            if (writeData.size() >= batchSize) {
                writeCondition.signal();
            }
        } finally {
            writeLock.unlock();
        }

        // 4. Return immediate success acknowledgment
        WriteResponse response = new WriteResponse();
        response.setRequestId(request.getRequestId());
        response.setCode("OK");
        response.setMessage(String.format("Accepted %d items for async write", itemCount));
        return response;
    }

    /**
     * Internal synchronous write logic (executed by background thread)
     */
    private WriteResponse doWrite(String instanceId, String table, WriteRequest request) throws RecallEngineException {
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
                    throw new RecallEngineException(errorMsg);
                }

                return objectMapper.readValue(responseBody, WriteResponse.class);
            }
        } catch (RecallEngineException e) {
            throw e;
        } catch (Exception e) {
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

    // ==================== Async Implementation Details ====================

    private ExecutorService getWriteExecutor() {
        ExecutorService executor = writeExecutor;
        if (executor != null && !executor.isShutdown()) {
            return executor;
        }
        synchronized (this) {
            executor = writeExecutor;
            if (executor != null && !executor.isShutdown()) {
                return executor;
            }
            // Refuse to recreate the executor once the client has been closed.
            // Without this guard, getWriteExecutor() would silently spin up a
            // new thread pool that nobody owns, leaking threads and accepting
            // writes that will never be observed by close()/writeFlush().
            if (!running) {
                throw new IllegalStateException(
                        "RecallEngineClient is closed; the async write executor cannot be recreated");
            }
            executor = Executors.newFixedThreadPool(writeThreadPoolSize);
            writeExecutor = executor;
            return executor;
        }
    }

    /**
     * Start the async write background thread.
     * Uses Double-Checked Locking for thread safety and performance:
     * - First check without lock (fast path for most calls)
     * - Only acquire lock when thread needs to be created
     */
    private void startAsyncWriteThread() {
        // Fast path: if thread is already running, return immediately (no lock needed)
        Thread thread = asyncWriteThread;
        if (thread != null && thread.isAlive()) {
            return;
        }

        // Slow path: need to create thread, use synchronized
        synchronized (this) {
            // Double-check after acquiring lock
            thread = asyncWriteThread;
            if (thread != null && thread.isAlive()) {
                return;
            }

            String threadName = "RecallEngineAsyncWriter";
            asyncWriteThread = new Thread(() -> {
                while (running) {
                    writeLock.lock();
                    try {
                        writeCondition.await(flushIntervalMs, TimeUnit.MILLISECONDS);
                        if (!writeData.isEmpty()) {
                            doAsyncWrite();
                        }
                    } catch (InterruptedException e) {
                        logger.warn("{} interrupted, flushing remaining data before exit", threadName);
                        Thread.currentThread().interrupt();
                        // Flush remaining data before exit
                        try {
                            if (!writeData.isEmpty()) {
                                doAsyncWrite();
                            }
                        } catch (Exception ex) {
                            logger.error("Failed to flush data on interrupt", ex);
                        }
                        break;
                    } finally {
                        writeLock.unlock();
                    }
                }
                // Handle remaining data after thread stops
                writeLock.lock();
                try {
                    if (!writeData.isEmpty()) {
                        doAsyncWrite();
                    }
                } finally {
                    writeLock.unlock();
                }
                logger.info("{} has stopped.", threadName);
            }, threadName);
            asyncWriteThread.setDaemon(true);
            asyncWriteThread.start();
        }
    }

    /**
     * Force flush remaining data and wait for the HTTP batch to complete.
     * <p>
     * The HTTP request is submitted while holding the buffer lock, but the
     * caller waits for completion <em>outside</em> the lock so that concurrent
     * producers ({@link #write}) and the background flush thread are not
     * blocked by network I/O. The wait is bounded by {@code flushTimeoutMs}
     * configured via {@link #withFlushTimeoutMs(long)}.
     */
    public void writeFlush() {
        Future<?> future = null;
        int pendingCount = 0;
        writeLock.lock();
        try {
            if (!writeData.isEmpty()) {
                pendingCount = writeData.size();
                logger.info("Write flush: {} items pending", pendingCount);
                // doAsyncWrite() drains writeData into a temp list and submits
                // to the executor; once it returns, the buffer is empty and
                // the lock can be released safely.
                future = doAsyncWrite();
            }
        } finally {
            writeLock.unlock();
        }

        if (future == null) {
            return;
        }

        try {
            future.get(flushTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            logger.error("Write flush timed out after {} ms with {} items in flight; cancelling",
                    flushTimeoutMs, pendingCount);
            future.cancel(true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            logger.warn("Write flush interrupted while waiting for completion");
        } catch (Exception e) {
            logger.error("Error waiting for write completion: {}", e.getMessage(), e);
        }
    }

    /**
     * Perform the actual async write
     *
     * @return Future for tracking completion
     */
    private Future<?> doAsyncWrite() {
        if (writeData.isEmpty()) {
            return null;
        }

        List<WriteItem> tempList = new ArrayList<>(writeData);
        writeData.clear();

        return getWriteExecutor().submit(() -> {
            // Group by Instance/Table to minimize requests
            Map<String, List<WriteItem>> grouped = new HashMap<>();
            for (WriteItem item : tempList) {
                String key = item.instanceId + "|" + item.table;
                grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
            }

            // Write each group
            for (List<WriteItem> items : grouped.values()) {
                if (items.isEmpty()) continue;

                String instId = items.get(0).instanceId;
                String tbl = items.get(0).table;

                try {
                    WriteRequest request = new WriteRequest();
                    List<Map<String, Object>> content = new ArrayList<>(items.size());
                    for (WriteItem item : items) {
                        content.add(item.data);
                    }
                    request.setContent(content);

                    // Execute actual HTTP call with retry
                    if (retryTimes > 0) {
                        RecallEngineException lastException = null;
                        for (int i = 0; i < retryTimes; i++) {
                            try {
                                doWrite(instId, tbl, request);
                                lastException = null;
                                break;
                            } catch (RecallEngineException e) {
                                lastException = e;
                                logger.warn("Async write failed, retrying ({}/{}), err: {}", i + 1, retryTimes, e.getMessage());
                            }
                        }
                        if (lastException != null) {
                            throw lastException;
                        }
                    } else {
                        doWrite(instId, tbl, request);
                    }

                    logger.debug("Async write completed: {} items to {}/{}", items.size(), instId, tbl);
                } catch (Exception e) {
                    logger.error("Async write failed for {}/{}: {}", instId, tbl, e.getMessage(), e);
                }
            }
        });
    }

    /**
     * Close client resources
     */
    public void close() {
        this.running = false;

        writeLock.lock();
        try {
            writeCondition.signalAll();
        } finally {
            writeLock.unlock();
        }

        // Wait for async write thread to stop
        if (asyncWriteThread != null && asyncWriteThread.isAlive()) {
            try {
                asyncWriteThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Flush remaining data
        writeFlush();

        // Shutdown executor
        if (writeExecutor != null && !writeExecutor.isShutdown()) {
            writeExecutor.shutdown();
            try {
                if (!writeExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    writeExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                writeExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    // ==================== Inner Class ====================

    private static class WriteItem {
        final String instanceId;
        final String table;
        final Map<String, Object> data;

        WriteItem(String instanceId, String table, Map<String, Object> data) {
            this.instanceId = instanceId;
            this.table = table;
            this.data = data;
        }
    }
}
