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
        if (executor == null || executor.isShutdown()) {
            synchronized (this) {
                executor = writeExecutor;
                if (executor == null || executor.isShutdown()) {
                    executor = Executors.newFixedThreadPool(writeThreadPoolSize);
                    writeExecutor = executor;
                }
            }
        }
        return executor;
    }

    private void startAsyncWriteThread() {
        Thread thread = asyncWriteThread;
        if (thread != null && thread.isAlive()) {
            return;
        }

        synchronized (this) {
            thread = asyncWriteThread;
            if (thread != null && thread.isAlive()) {
                return;
            }

            String threadName = "RecallEngineAsyncWriter";
            asyncWriteThread = new Thread(() -> {
                while (running) {
                    writeLock.lock();
                    try {
                        // Wait for timeout or signal
                        writeCondition.await(flushIntervalMs, TimeUnit.MILLISECONDS);
                        if (!writeData.isEmpty()) {
                            doAsyncWrite();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } finally {
                        writeLock.unlock();
                    }
                }
                // Final flush
                writeLock.lock();
                try {
                    if (!writeData.isEmpty()) {
                        doAsyncWrite();
                    }
                } finally {
                    writeLock.unlock();
                }
            }, threadName);
            asyncWriteThread.setDaemon(true);
            asyncWriteThread.start();
        }
    }

    /**
     * Force flush remaining data (Synchronous wait)
     */
    public void writeFlush() {
        writeLock.lock();
        try {
            if (!writeData.isEmpty()) {
                Future<?> future = doAsyncWrite();
                if (future != null) {
                    try {
                        future.get(5, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        logger.error("Flush wait error", e);
                    }
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    private Future<?> doAsyncWrite() {
        if (writeData.isEmpty()) {
            return null;
        }

        // Snapshot data
        List<WriteItem> tempList = new ArrayList<>(writeData);
        writeData.clear();

        return getWriteExecutor().submit(() -> {
            // Group by Instance/Table to minimize requests
            Map<String, List<WriteItem>> grouped = new HashMap<>();
            for (WriteItem item : tempList) {
                String key = item.instanceId + "|" + item.table;
                grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
            }

            // Send each group
            for (List<WriteItem> items : grouped.values()) {
                if (items.isEmpty()) continue;

                String instId = items.get(0).instanceId;
                String tbl = items.get(0).table;

                try {
                    WriteRequest req = new WriteRequest();
                    List<Map<String, Object>> content = new ArrayList<>(items.size());
                    for (WriteItem item : items) {
                        content.add(item.data);
                    }
                    req.setContent(content);

                    // Execute actual HTTP call with retry
                    if (retryTimes > 0) {
                        RecallEngineException lastException = null;
                        for (int i = 0; i < retryTimes; i++) {
                            try {
                                doWrite(instId, tbl, req);
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
                        doWrite(instId, tbl, req);
                    }

                } catch (Exception e) {
                    logger.error("Async write FAILED for {}/{}. Count: {}. Error: {}",
                            instId, tbl, items.size(), e.getMessage(), e);
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

        if (asyncWriteThread != null && asyncWriteThread.isAlive()) {
            try {
                asyncWriteThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        writeFlush();

        if (writeExecutor != null && !writeExecutor.isShutdown()) {
            writeExecutor.shutdown();
            try {
                if (!writeExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    writeExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                writeExecutor.shutdownNow();
            }
        }

        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
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
