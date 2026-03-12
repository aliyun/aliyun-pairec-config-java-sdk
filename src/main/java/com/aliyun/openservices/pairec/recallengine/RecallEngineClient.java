package com.aliyun.openservices.pairec.recallengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * RecallEngine Client for making recall and write requests
 */
public class RecallEngineClient {
    public static final Logger logger = LoggerFactory.getLogger(RecallEngineClient.class);
    
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final int DEFAULT_TIMEOUT_MS = 500;
    
    private String endpoint;
    private String username;
    private String password;
    int retryTimes;
    Map<String, String> requestHeaders;
    private OkHttpClient httpClient;
    private String authCache;
    private ObjectMapper objectMapper;

    // Async write fields
    private final List<WriteItem> writeData = new ArrayList<>();
    private final ReentrantLock writeLock = new ReentrantLock();
    private final Condition writeCondition = writeLock.newCondition();
    private final ExecutorService writeExecutor = Executors.newFixedThreadPool(4);
    private volatile boolean running = true;
    private Thread asyncWriteThread;
    private int batchSize = 20;
    private long flushIntervalMs = 50;
    private WriteCallback globalCallback;
    
    /**
     * Create a new RecallEngineClient
     * 
     * @param endpoint the endpoint URL
     * @param username the username for authentication
     * @param password the password for authentication
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
    
    /**
     * Set the number of retry attempts
     * 
     * @param retryTimes number of retries
     * @return this client for method chaining
     */
    public RecallEngineClient withRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
        return this;
    }
    
    /**
     * Set a request header
     * 
     * @param key header name
     * @param value header value
     * @return this client for method chaining
     */
    public RecallEngineClient withRequestHeader(String key, String value) {
        this.requestHeaders.put(key, value);
        return this;
    }
    
    /**
     * Set custom HTTP client
     * 
     * @param httpClient the HTTP client
     * @return this client for method chaining
     */
    public RecallEngineClient withHttpClient(OkHttpClient httpClient) {
        this.httpClient = httpClient;
        return this;
    }
    
    /**
     * Make a recall request
     * 
     * @param request the recall request
     * @return the recall response
     * @throws RecallEngineException if the request fails
     */
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
            
            // Add custom headers
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
    
    /**
     * Make an async write request
     * Data is buffered and written in background batches.
     * Returns immediately with a response indicating the request was accepted.
     *
     * @param instanceId the instance ID
     * @param table the table name
     * @param request the write request
     * @return WriteResponse indicating the request was accepted
     */
    public WriteResponse write(String instanceId, String table, WriteRequest request) {
        startAsyncWriteThread();

        if (request == null || request.getContent() == null || request.getContent().isEmpty()) {
            WriteResponse response = new WriteResponse();
            response.setRequestId(request != null ? request.getRequestId() : null);
            response.setCode("OK");
            response.setMessage("Empty request, nothing to write");
            return response;
        }

        int itemCount = request.getContent().size();

        writeLock.lock();
        try {
            for (Map<String, Object> data : request.getContent()) {
                writeData.add(new WriteItem(instanceId, table, data));
            }
            if (writeData.size() >= batchSize) {
                writeCondition.signal();
            }
        } finally {
            writeLock.unlock();
        }

        WriteResponse response = new WriteResponse();
        response.setRequestId(request.getRequestId());
        response.setCode("OK");
        response.setMessage(String.format("Accepted %d items for async write", itemCount));
        return response;
    }

    /**
     * Make an async write request with callback
     * Data is buffered and written in background batches.
     * Returns immediately with a response indicating the request was accepted.
     * The callback will be invoked when the write operation completes.
     *
     * @param instanceId the instance ID
     * @param table the table name
     * @param request the write request
     * @param callback the callback to be invoked on write completion
     * @return WriteResponse indicating the request was accepted
     */
    public WriteResponse write(String instanceId, String table, WriteRequest request, WriteCallback callback) {
        startAsyncWriteThread();

        if (request == null || request.getContent() == null || request.getContent().isEmpty()) {
            WriteResponse response = new WriteResponse();
            response.setRequestId(request != null ? request.getRequestId() : null);
            response.setCode("OK");
            response.setMessage("Empty request, nothing to write");
            return response;
        }

        int itemCount = request.getContent().size();

        writeLock.lock();
        try {
            for (Map<String, Object> data : request.getContent()) {
                writeData.add(new WriteItem(instanceId, table, data, callback));
            }
            if (writeData.size() >= batchSize) {
                writeCondition.signal();
            }
        } finally {
            writeLock.unlock();
        }

        WriteResponse response = new WriteResponse();
        response.setRequestId(request.getRequestId());
        response.setCode("OK");
        response.setMessage(String.format("Accepted %d items for async write", itemCount));
        return response;
    }
    
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
            
            // Add custom headers
            for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
                requestBuilder.header(entry.getKey(), entry.getValue());
            }
            
            try (okhttp3.Response response = httpClient.newCall(requestBuilder.build()).execute()) {
                if (response.body() == null) {
                    throw new RecallEngineException("Empty response body");
                }
                
                String responseBody = response.body().string();
                
                if (response.code() != 200) {
                    String errorMsg = "write request failed, response status code: " + response.code();
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> errorMap = objectMapper.readValue(responseBody, Map.class);
                        if (errorMap.containsKey("message")) {
                            errorMsg = "write request failed, response status code: " + response.code() + 
                                       ", message: " + errorMap.get("message");
                        }
                    } catch (Exception e) {
                        logger.debug("Failed to parse error response", e);
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

    // ==================== Async Write Methods ====================

    /**
     * Configure async write batch size
     *
     * @param batchSize batch size for async write (default: 20)
     * @return this client for method chaining
     */
    public RecallEngineClient withBatchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    /**
     * Configure async write flush interval
     *
     * @param flushIntervalMs flush interval in milliseconds (default: 50)
     * @return this client for method chaining
     */
    public RecallEngineClient withFlushInterval(long flushIntervalMs) {
        this.flushIntervalMs = flushIntervalMs;
        return this;
    }

    /**
     * Set a global callback for all async write operations
     *
     * @param callback the callback to be invoked on write completion
     * @return this client for method chaining
     */
    public RecallEngineClient withWriteCallback(WriteCallback callback) {
        this.globalCallback = callback;
        return this;
    }

    /**
     * Start the async write background thread
     */
    private void startAsyncWriteThread() {
        if (asyncWriteThread != null && asyncWriteThread.isAlive()) {
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
                    Thread.currentThread().interrupt();
                    break;
                } finally {
                    writeLock.unlock();
                }
            }
            logger.info(threadName + " has stopped.");
        }, threadName);
        asyncWriteThread.setDaemon(true);
        asyncWriteThread.start();
    }

    /**
     * Flush all pending data and wait for completion
     */
    public void writeFlush() {
        writeLock.lock();
        try {
            if (!writeData.isEmpty()) {
                logger.info("Write flush: {} items pending", writeData.size());
                Future<?> future = doAsyncWrite();
                if (future != null) {
                    try {
                        future.get();
                    } catch (Exception e) {
                        logger.error("Error waiting for write completion: {}", e.getMessage());
                    }
                }
            }

            writeExecutor.shutdown();
            try {
                if (!writeExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    writeExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                writeExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        } finally {
            writeLock.unlock();
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

        return writeExecutor.submit(() -> {
            // Group by instanceId, table, and callback
            Map<String, List<WriteItem>> groupedItems = new HashMap<>();
            for (WriteItem item : tempList) {
                // Include callback in the key to group items with same callback together
                String key = item.instanceId + ":" + item.table + ":" + System.identityHashCode(item.callback);
                groupedItems.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
            }

            // Write each group
            for (Map.Entry<String, List<WriteItem>> entry : groupedItems.entrySet()) {
                String[] parts = entry.getKey().split(":", 3);
                String instanceId = parts[0];
                String table = parts[1];
                List<WriteItem> items = entry.getValue();

                // Get callback from first item (all items in group have same callback)
                WriteCallback callback = items.get(0).callback;
                // Use global callback if no specific callback
                if (callback == null) {
                    callback = globalCallback;
                }

                try {
                    WriteRequest request = new WriteRequest();
                    List<Map<String, Object>> content = new ArrayList<>(items.size());
                    for (WriteItem item : items) {
                        content.add(item.data);
                    }
                    request.setContent(content);

                    WriteResponse response = doWrite(instanceId, table, request);
                    logger.debug("Async write completed: {} items to {}/{}", items.size(), instanceId, table);

                    // Invoke success callback
                    if (callback != null) {
                        try {
                            callback.onSuccess(instanceId, table, response);
                        } catch (Exception e) {
                            logger.error("Callback onError raised exception: {}", e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    logger.error("Async write failed for {}/{}: {}", instanceId, table, e.getMessage());

                    // Invoke error callback
                    if (callback != null) {
                        try {
                            callback.onError(instanceId, table, e);
                        } catch (Exception ex) {
                            logger.error("Callback onError raised exception: {}", ex.getMessage());
                        }
                    }
                }
            }
        });
    }

    /**
     * Close the client and release resources
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

        // Shutdown executors
        if (!writeExecutor.isShutdown()) {
            writeExecutor.shutdownNow();
        }
    }

    /**
     * Internal class to hold write data items
     */
    private static class WriteItem {
        final String instanceId;
        final String table;
        final Map<String, Object> data;
        final long timestamp;
        final WriteCallback callback;

        WriteItem(String instanceId, String table, Map<String, Object> data) {
            this(instanceId, table, data, null);
        }

        WriteItem(String instanceId, String table, Map<String, Object> data, WriteCallback callback) {
            this.instanceId = instanceId;
            this.table = table;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
            this.callback = callback;
        }
    }
}
