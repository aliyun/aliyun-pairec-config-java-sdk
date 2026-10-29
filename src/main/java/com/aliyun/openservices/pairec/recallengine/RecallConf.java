package com.aliyun.openservices.pairec.recallengine;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * RecallConf represents recall configuration
 */
public class RecallConf {
    @JsonProperty("trigger")
    private String trigger;
    
    @JsonProperty("count")
    private int count;
    
    @JsonProperty("options")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private RecallOptions options;

    @JsonProperty("queries")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private float[][] queries;

    @JsonProperty("version_id")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String versionId;

    @JsonProperty("user_embedding_version_id")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String userEmbeddingVersionId;

    public RecallConf() {
    }
    
    public RecallConf(String trigger, int count) {
        this.trigger = trigger;
        this.count = count;
    }
    
    public String getTrigger() {
        return trigger;
    }
    
    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }
    
    public int getCount() {
        return count;
    }
    
    public void setCount(int count) {
        this.count = count;
    }

    public RecallOptions getOptions() {
        return options;
    }

    public void setOptions(RecallOptions options) {
        this.options = options;
    }

    public float[][] getQueries() {
        return queries;
    }

    /**
     * Set explicit query vectors for vector recall. Null or empty arrays are omitted.
     * Nonempty queries cannot be combined with a trigger, user embedding parameters,
     * or a nonzero trigger limit.
     *
     * @param queries query vectors
     */
    public void setQueries(float[][] queries) {
        this.queries = queries;
    }

    public String getVersionId() {
        return versionId;
    }

    /**
     * Set the item vector table version, independently of the recall service version.
     *
     * @param versionId item vector table version ID; null or empty uses the default
     */
    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public String getUserEmbeddingVersionId() {
        return userEmbeddingVersionId;
    }

    /**
     * Set the user embedding table version for trigger-based vector recall.
     *
     * @param userEmbeddingVersionId user embedding table version ID;
     *                               null or empty uses the default
     */
    public void setUserEmbeddingVersionId(String userEmbeddingVersionId) {
        this.userEmbeddingVersionId = userEmbeddingVersionId;
    }
}
