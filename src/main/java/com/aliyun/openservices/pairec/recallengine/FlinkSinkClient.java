package com.aliyun.openservices.pairec.recallengine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.ConnectionPool;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.flink.annotation.Internal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Connector adapter for the synchronous, buffered client bundled on the Flink branch. */
@Internal
public final class FlinkSinkClient extends RecallEngineClient {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final ObjectMapper mapper = new ObjectMapper();
    private final OkHttpClient http;
    private final HttpUrl endpoint;
    private final String auth;
    private final int attempts;
    private final boolean strictWrites;
    private volatile IllegalStateException writeFailure;

    public FlinkSinkClient(String endpoint, String username, String password, int retryTimes, boolean strictWrites) {
        super(endpoint, username, password);
        this.endpoint = HttpUrl.get(endpoint.startsWith("https://") || endpoint.startsWith("http://")
                ? endpoint : "http://" + endpoint);
        this.auth = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        this.strictWrites = strictWrites;
        this.attempts = Math.max(1, retryTimes);
        this.http = new OkHttpClient.Builder()
                .connectTimeout(200, TimeUnit.MILLISECONDS)
                .readTimeout(500, TimeUnit.MILLISECONDS)
                .writeTimeout(500, TimeUnit.MILLISECONDS)
                .connectionPool(new ConnectionPool(1000, 5, TimeUnit.MINUTES))
                .build();
        withHttpClient(http);
        // A delete-enabled sink records the failure after the final HTTP attempt.
        withRetryTimes(strictWrites ? 1 : retryTimes);
    }

    @Override
    WriteResponse doWrite(String instanceId, String table, WriteRequest request) throws RecallEngineException {
        if (!strictWrites) {
            return super.doWrite(instanceId, table, request);
        }
        checkWriteFailure();
        try {
            WriteResponse response = withRetry(() -> super.doWrite(instanceId, table, request));
            if (response == null || !"OK".equals(response.getCode())) {
                throw new RecallEngineException(response == null ? "Empty write response" : response.getMessage());
            }
            return response;
        } catch (RecallEngineException e) {
            writeFailure = new IllegalStateException("Buffered write failed", e);
            throw e;
        }
    }

    @Override
    public WriteResponse write(String instanceId, String table, WriteRequest request) {
        checkWriteFailure();
        WriteResponse response = super.write(instanceId, table, request);
        checkWriteFailure();
        return response;
    }

    @Override
    public void writeFlush() {
        super.writeFlush();
        checkWriteFailure();
    }

    private void checkWriteFailure() {
        if (writeFailure != null) {
            throw writeFailure;
        }
    }

    public void deleteGroup(String instanceId, String table, String pkey) throws RecallEngineException {
        delete(instanceId, table, "delete_records", Collections.singletonMap("keys", Collections.singletonList(pkey)));
    }

    public void deleteMember(String instanceId, String table, String pkey, String skey) throws RecallEngineException {
        Map<String, String> pair = new HashMap<>();
        pair.put("pkey", pkey);
        pair.put("skey", skey);
        delete(instanceId, table, "delete_key_pairs", Collections.singletonMap("key_pairs", Collections.singletonList(pair)));
    }

    private void delete(String instanceId, String table, String action, Map<String, ?> payload) throws RecallEngineException {
        HttpUrl url = endpoint.newBuilder().addPathSegments("api/v1/tables").addPathSegment(instanceId)
                .addPathSegment("default").addPathSegment(table).addPathSegment(action).build();
        Request.Builder request = new Request.Builder().url(url).header("Auth", auth);
        requestHeaders.forEach(request::header);
        try {
            request.post(RequestBody.create(mapper.writeValueAsBytes(payload), JSON));
        } catch (IOException e) {
            throw new RecallEngineException("Could not encode delete request", e);
        }
        Request post = request.build();
        Response result = withRetry(() -> {
            try (okhttp3.Response response = http.newCall(post).execute()) {
                if (!response.isSuccessful()) {
                    String message = "Delete request failed: HTTP " + response.code();
                    try {
                        JsonNode body = mapper.readTree(response.peekBody(4096).byteStream());
                        if (body != null && body.path("message").isTextual()) {
                            message += ", " + body.get("message").textValue();
                        }
                    } catch (IOException e) {
                        logger.debug("Could not read delete error details", e);
                    }
                    throw new RecallEngineException(message, response.code());
                }
                return response.body() == null ? null : mapper.readValue(response.body().byteStream(), Response.class);
            } catch (IOException e) {
                throw new RecallEngineException("Delete request failed", e);
            }
        });
        if (result == null || !"OK".equals(result.getCode())) {
            throw new RecallEngineException(result == null ? "Empty delete response" : result.getMessage());
        }
    }

    private interface Operation<T> {
        T run() throws RecallEngineException;
    }

    private <T> T withRetry(Operation<T> operation) throws RecallEngineException {
        for (int n = 1; ; n++) {
            try {
                return operation.run();
            } catch (RecallEngineException e) {
                int status = e.getStatusCode();
                if (n >= attempts || (status != RecallEngineException.NO_STATUS_CODE && status != 429 && status < 500)) {
                    throw e;
                }
            }
        }
    }
}
