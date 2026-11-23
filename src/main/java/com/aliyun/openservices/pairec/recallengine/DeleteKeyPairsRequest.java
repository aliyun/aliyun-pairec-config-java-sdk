package com.aliyun.openservices.pairec.recallengine;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Deletes the specified members of a v2 KKV table. */
public class DeleteKeyPairsRequest {
    public static final class KeyPair {
        public final String pkey;
        public final String skey;

        public KeyPair(String pkey, String skey) {
            this.pkey = pkey;
            this.skey = skey;
        }
    }

    @JsonProperty("key_pairs")
    private final List<KeyPair> keyPairs;
    @JsonProperty("version_id")
    private String versionId;
    @JsonProperty("request_id")
    private String requestId;
    private String schema = DeleteRequest.DEFAULT_SCHEMA;

    public DeleteKeyPairsRequest(List<KeyPair> keyPairs) { this.keyPairs = keyPairs; }
    public String getSchema() { return schema; }
    public void setSchema(String schema) {
        this.schema = schema == null || schema.isEmpty() ? DeleteRequest.DEFAULT_SCHEMA : schema;
    }
    public void setVersionId(String versionId) { this.versionId = versionId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    void validate() {
        if (keyPairs == null || keyPairs.isEmpty() || keyPairs.size() > DeleteRequest.MAX_DELETE_KEYS_COUNT) {
            throw new IllegalArgumentException("Expected 1 to 10000 key pairs");
        }
        for (KeyPair pair : keyPairs) {
            if (pair == null || pair.pkey == null || pair.pkey.isEmpty() || pair.skey == null || pair.skey.isEmpty()) {
                throw new IllegalArgumentException("Both pkey and skey are required");
            }
        }
    }
}
