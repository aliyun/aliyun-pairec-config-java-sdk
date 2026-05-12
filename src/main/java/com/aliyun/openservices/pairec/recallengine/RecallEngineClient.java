package com.aliyun.openservices.pairec.recallengine;

import com.aliyun.pairecservice20221213.Client;
import com.aliyun.pairecservice20221213.models.GetRecallManagementConfigRequest;
import com.aliyun.pairecservice20221213.models.GetRecallManagementConfigResponse;
import com.aliyun.pairecservice20221213.models.GetRecallManagementConfigResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

    private volatile String endpoint;
    private String username;
    private String password;
    private String region;
    private String instanceId;
    private String accessKeyId;
    private String accessKeySecret;
    private volatile boolean usePublicEndpoint = false;
    private volatile boolean publicEndpointFetched = false;
    int retryTimes;
    final Map<String, String> requestHeaders;
    private OkHttpClient httpClient;
    private String authCache;
    private ObjectMapper objectMapper;

    // Async write fields
    private final List<WriteItem> writeData = new ArrayList<>();
    private final ReentrantLock writeLock = new ReentrantLock();
    private final Condition writeCondition = writeLock.newCondition();
    private volatile ExecutorService writeExecutor;
    private volatile boolean running = true;
    private volatile Thread asyncWriteThread;
    private int batchSize = 20;
    private long flushIntervalMs = 50;
    private WriteCallback globalCallback;
    private int writeThreadPoolSize = 4;
    
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
        this.requestHeaders = new ConcurrentHashMap<>();
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
     * Set the region for the client
     *
     * @param region the region
     * @return this client for method chaining
     */
    public RecallEngineClient withRegion(String region) {
        this.region = region;
        this.publicEndpointFetched = false;
        return this;
    }

    /**
     * Set the instance ID for public endpoint resolution
     *
     * @param instanceId the instance ID
     * @return this client for method chaining
     */
    public RecallEngineClient withInstanceId(String instanceId) {
        this.instanceId = instanceId;
        this.publicEndpointFetched = false;
        return this;
    }

    /**
     * Set the AccessKey ID for OpenAPI authentication
     *
     * @param accessKeyId the AccessKey ID
     * @return this client for method chaining
     */
    public RecallEngineClient withAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
        return this;
    }

    /**
     * Set the AccessKey Secret for OpenAPI authentication
     *
     * @param accessKeySecret the AccessKey Secret
     * @return this client for method chaining
     */
    public RecallEngineClient withAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
        return this;
    }

    /**
     * Set whether to use public endpoint.
     * When enabled, the client will call getRecallManagementConfig OpenAPI
     * to fetch the public endpoint and token.
     *
     * @param usePublicEndpoint true to use public endpoint
     * @return this client for method chaining
     */
    public RecallEngineClient withPublicEndpoint(boolean usePublicEndpoint) {
        this.usePublicEndpoint = usePublicEndpoint;
        this.publicEndpointFetched = false;
        return this;
    }

    /**
     * Fetch public endpoint and token from getRecallManagementConfig OpenAPI
     */
    private void fetchPublicEndpointConfig() {
        if (!usePublicEndpoint || region == null || region.isEmpty() || instanceId == null || instanceId.isEmpty()) {
            return;
        }
        if (publicEndpointFetched) {
            return;
        }
        synchronized (this) {
            if (publicEndpointFetched) {
                return;
            }
            try {
                Config config = new Config();
                config.setAccessKeyId(accessKeyId);
                config.setAccessKeySecret(accessKeySecret);
                config.setRegionId(region);
                config.setEndpoint("pairecservice." + region + ".aliyuncs.com");

                logger.info("Fetching public endpoint config: region={}, instanceId={}, endpoint={}",
                    region, instanceId, config.getEndpoint());

                Client apiClient = new Client(config);
                GetRecallManagementConfigRequest request = new GetRecallManagementConfigRequest();
                request.setInstanceId(instanceId);

                GetRecallManagementConfigResponse response = apiClient.getRecallManagementConfig(request);
                GetRecallManagementConfigResponseBody body = response.getBody();

                logger.debug("getRecallManagementConfig response: statusCode={}, body={}",
                    response.getStatusCode(), body);

                if (body != null && body.getNetworkConfigs() != null && !body.getNetworkConfigs().isEmpty()) {
                    for (GetRecallManagementConfigResponseBody.GetRecallManagementConfigResponseBodyNetworkConfigs networkConfig : body.getNetworkConfigs()) {
                        logger.info("NetworkConfig: publicEndpoint={}, token={}, status={}, privateLinkAddress={}",
                            networkConfig.getPublicEndpoint(),
                            networkConfig.getToken() != null ? "***(length=" + networkConfig.getToken().length() + ")" : "null",
                            networkConfig.getStatus(),
                            networkConfig.getPrivateLinkAddress());
                        if (networkConfig.getPublicEndpoint() != null && !networkConfig.getPublicEndpoint().isEmpty()) {
                            this.endpoint = networkConfig.getPublicEndpoint();
                            if (networkConfig.getToken() != null) {
                                this.requestHeaders.put("Authorization", networkConfig.getToken());
                            }
                            this.publicEndpointFetched = true;
                            logger.info("Fetched public endpoint for region {}: {}", region, endpoint);
                            return;
                        }
                    }
                    logger.warn("No public endpoint found in networkConfigs for instance {}", instanceId);
                } else {
                    logger.warn("Empty networkConfigs returned for instance {}", instanceId);
                }
            } catch (Exception e) {
                logger.error("Failed to fetch public endpoint config: {}", e.getMessage(), e);
            }
        }
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
        validatePublicEndpointConfig();
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
        validatePublicEndpointConfig();
        startAsyncWriteThread();

        if (request == null || request.getContent() == null || request.getContent().isEmpty()) {
            WriteResponse response = new WriteResponse();
            response.setRequestId(request != null ? request.getRequestId() : null);
            response.setCode("OK");
            response.setMessage("Empty request, nothing to write");
            return response;
        }

        int itemCount = request.getContent().size();
        InsertMode insertMode = request.getInsertMode();

        writeLock.lock();
        try {
            for (Map<String, Object> data : request.getContent()) {
                writeData.add(new WriteItem(instanceId, table, data, null, insertMode));
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
        validatePublicEndpointConfig();
        startAsyncWriteThread();

        if (request == null || request.getContent() == null || request.getContent().isEmpty()) {
            WriteResponse response = new WriteResponse();
            response.setRequestId(request != null ? request.getRequestId() : null);
            response.setCode("OK");
            response.setMessage("Empty request, nothing to write");
            return response;
        }

        int itemCount = request.getContent().size();
        InsertMode insertMode = request.getInsertMode();

        writeLock.lock();
        try {
            for (Map<String, Object> data : request.getContent()) {
                writeData.add(new WriteItem(instanceId, table, data, callback, insertMode));
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
        String url = String.format("%s/api/v1/tables/%s/default/%s/write", endpoint, instanceId, table);
        return doPost(url, request, "write", WriteResponse.class);
    }

    // ==================== Delete Methods ====================

    /**
     * Delete data from a table
     * This is a synchronous operation.
     *
     * @param instanceId the instance ID
     * @param table the table name
     * @param request the delete request containing primary key conditions
     * @return DeleteResponse indicating the result
     * @throws RecallEngineException if the request fails
     */
    public DeleteResponse delete(String instanceId, String table, DeleteRequest request) throws RecallEngineException {
        validatePublicEndpointConfig();
        request.validate();

        if (retryTimes > 0) {
            RecallEngineException lastException = null;
            for (int i = 0; i < retryTimes; i++) {
                try {
                    return doDelete(instanceId, table, request);
                } catch (RecallEngineException e) {
                    lastException = e;
                    logger.warn("recallengine: delete failed, retrying..., err: {}", e.getMessage());
                }
            }
            throw lastException;
        } else {
            return doDelete(instanceId, table, request);
        }
    }

    private DeleteResponse doDelete(String instanceId, String table, DeleteRequest request) throws RecallEngineException {
        String url = String.format("%s/api/v1/tables/%s/%s/%s/delete_records", endpoint, instanceId, request.getSchema(), table);
        return doPost(url, request, "delete", DeleteResponse.class);
    }

    /**
     * Common POST request method
     *
     * @param url the request URL
     * @param payload the request body object
     * @param operationName operation name for error messages
     * @param responseClass the response type to deserialize into
     * @return the deserialized response
     * @throws RecallEngineException if the request fails
     */
    private <T> T doPost(String url, Object payload, String operationName, Class<T> responseClass) throws RecallEngineException {
        try {
            String json = objectMapper.writeValueAsString(payload);
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
                    String errorMsg = operationName + " request failed, response status code: " + response.code();
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> errorMap = objectMapper.readValue(responseBody, Map.class);
                        if (errorMap.containsKey("message")) {
                            errorMsg = operationName + " request failed, response status code: " + response.code() +
                                       ", message: " + errorMap.get("message");
                        }
                    } catch (Exception e) {
                        logger.debug("Failed to parse error response", e);
                    }
                    throw new RecallEngineException(errorMsg);
                }

                return objectMapper.readValue(responseBody, responseClass);
            }
        } catch (RecallEngineException e) {
            throw e;
        } catch (Exception e) {
            throw new RecallEngineException(operationName + " request failed", e);
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
     * Validate public endpoint configuration before making requests
     *
     * @throws IllegalStateException if usePublicEndpoint is true but required config is missing
     */
    private void validatePublicEndpointConfig() {
        if (usePublicEndpoint) {
            if (region == null || region.isEmpty()) {
                throw new IllegalStateException(
                    "Region must be set when using public endpoint. Call withRegion() to set the region."
                );
            }
            if (instanceId == null || instanceId.isEmpty()) {
                throw new IllegalStateException(
                    "InstanceId must be set when using public endpoint. Call withInstanceId() to set the instance ID."
                );
            }
            if (accessKeyId == null || accessKeyId.isEmpty() || accessKeySecret == null || accessKeySecret.isEmpty()) {
                throw new IllegalStateException(
                    "AccessKeyId and AccessKeySecret must be set when using public endpoint. " +
                    "Call withAccessKeyId() and withAccessKeySecret() to set them."
                );
            }
            if (!publicEndpointFetched) {
                fetchPublicEndpointConfig();
            }
            if (!publicEndpointFetched) {
                throw new IllegalStateException(
                    "Failed to fetch public endpoint config for region: " + region + ", instanceId: " + instanceId
                );
            }
        }
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
     * Configure write thread pool size
     *
     * @param poolSize thread pool size for async write (default: 4)
     * @return this client for method chaining
     */
    public RecallEngineClient withWriteThreadPoolSize(int poolSize) {
        this.writeThreadPoolSize = poolSize;
        return this;
    }

    /**
     * Get or create the write executor using Double-Checked Locking for thread safety and performance.
     * This pattern ensures:
     * - Thread safety: only one thread creates the executor
     * - Performance: subsequent calls only need a volatile read (no synchronization)
     */
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

        return getWriteExecutor().submit(() -> {
            // Group by instanceId, table, callback, and insertMode using a composite key object
            Map<GroupKey, List<WriteItem>> groupedItems = new HashMap<>();
            for (WriteItem item : tempList) {
                GroupKey key = new GroupKey(item.instanceId, item.table, 
                                            System.identityHashCode(item.callback), item.insertMode);
                groupedItems.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
            }

            // Write each group
            for (Map.Entry<GroupKey, List<WriteItem>> entry : groupedItems.entrySet()) {
                GroupKey key = entry.getKey();
                String instanceId = key.instanceId;
                String table = key.table;
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
                    request.setInsertMode(items.get(0).insertMode);

                    WriteResponse response = doWrite(instanceId, table, request);
                    logger.debug("Async write completed: {} items to {}/{}", items.size(), instanceId, table);

                    // Invoke success callback
                    if (callback != null) {
                        try {
                            callback.onSuccess(instanceId, table, response);
                        } catch (Exception e) {
                            logger.error("Callback onSuccess raised exception", e);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Async write failed for {}/{}: {}", instanceId, table, e.getMessage(), e);

                    // Invoke error callback
                    if (callback != null) {
                        try {
                            callback.onError(instanceId, table, e);
                        } catch (Exception ex) {
                            logger.error("Callback onError raised exception", ex);
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

    /**
     * Internal class to hold write data items
     */
    private static class WriteItem {
        final String instanceId;
        final String table;
        final Map<String, Object> data;
        final WriteCallback callback;
        final InsertMode insertMode;

        WriteItem(String instanceId, String table, Map<String, Object> data, WriteCallback callback, InsertMode insertMode) {
            this.instanceId = instanceId;
            this.table = table;
            this.data = data;
            this.callback = callback;
            this.insertMode = insertMode;
        }
    }

    /**
     * Internal class for grouping write items by instanceId, table, callback, and insertMode.
     * Using a proper key object avoids string parsing issues when instanceId or table contains ":".
     */
    private static class GroupKey {
        final String instanceId;
        final String table;
        final int callbackHash;
        final InsertMode insertMode;

        GroupKey(String instanceId, String table, int callbackHash, InsertMode insertMode) {
            this.instanceId = instanceId;
            this.table = table;
            this.callbackHash = callbackHash;
            this.insertMode = insertMode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GroupKey groupKey = (GroupKey) o;
            return callbackHash == groupKey.callbackHash &&
                   java.util.Objects.equals(instanceId, groupKey.instanceId) &&
                   java.util.Objects.equals(table, groupKey.table) &&
                   insertMode == groupKey.insertMode;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(instanceId, table, callbackHash, insertMode);
        }
    }
}
