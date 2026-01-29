package com.aliyun.openservices.pairec.recallengine;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * WriteRequest represents a write request
 */
public class WriteRequest {
    @JsonProperty("request_id")
    private String requestId;
    
    @JsonProperty("content")
    private List<Map<String, Object>> content;
    
    private String versionId;
    
    public WriteRequest() {
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public List<Map<String, Object>> getContent() {
        return content;
    }
    
    public void setContent(List<Map<String, Object>> content) {
        this.content = content;
    }
    
    public String getVersionId() {
        return versionId;
    }
    
    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }
}
