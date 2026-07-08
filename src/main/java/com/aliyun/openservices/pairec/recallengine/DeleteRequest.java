package com.aliyun.openservices.pairec.recallengine;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DeleteRequest represents a delete request for table data
 */
public class DeleteRequest {
    /**
     * Maximum number of keys allowed in a single delete request
     */
    public static final int MAX_DELETE_KEYS_COUNT = 10000;

    @JsonProperty("request_id")
    private String requestId;

    /**
     * List of keys to delete (primary key values)
     */
    @JsonProperty("keys")
    private List<String> keys;

    /**
     * Version ID for the delete operation
     */
    @JsonProperty("version_id")
    private String versionId;

    /**
     * Default schema name used when none is specified
     */
    public static final String DEFAULT_SCHEMA = "default";

    /**
     * Schema name (default: "default")
     */
    @JsonProperty("schema")
    private String schema = DEFAULT_SCHEMA;

    public DeleteRequest() {
    }

    /**
     * Create a DeleteRequest with keys
     *
     * @param keys the keys to delete
     */
    public DeleteRequest(List<String> keys) {
        this.keys = keys;
    }

    /**
     * Get the request ID
     *
     * @return the request ID
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * Set the request ID
     *
     * @param requestId the request ID
     */
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    /**
     * Get the keys to delete
     *
     * @return the keys list
     */
    public List<String> getKeys() {
        return keys;
    }

    /**
     * Set the keys to delete
     *
     * @param keys the keys list
     */
    public void setKeys(List<String> keys) {
        this.keys = keys;
    }

    /**
     * Get the version ID
     *
     * @return the version ID
     */
    public String getVersionId() {
        return versionId;
    }

    /**
     * Set the version ID
     *
     * @param versionId the version ID
     */
    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    /**
     * Get the schema name
     *
     * @return the schema name
     */
    public String getSchema() {
        return schema;
    }

    /**
     * Set the schema name. If {@code null} or empty is supplied, the schema is
     * reset to {@link #DEFAULT_SCHEMA} to avoid producing malformed request URLs.
     *
     * @param schema the schema name
     */
    public void setSchema(String schema) {
        this.schema = (schema == null || schema.isEmpty()) ? DEFAULT_SCHEMA : schema;
    }

    /**
     * Validate the request
     *
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("keys is empty");
        }
        if (keys.size() > MAX_DELETE_KEYS_COUNT) {
            throw new IllegalArgumentException(
                String.format("keys count %d exceeds limit %d", keys.size(), MAX_DELETE_KEYS_COUNT));
        }
    }
}
