package com.aliyun.openservices.pairec.recallengine;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * RecallRequest represents a recall request
 */
public class RecallRequest {
    @JsonProperty("request_id")
    private String requestId;
    
    @JsonProperty("instance_id")
    private String instanceId;
    
    @JsonProperty("service")
    private String service;
    
    @JsonProperty("version")
    private String version;
    
    @JsonProperty("uid")
    private String uid;
    
    @JsonProperty("recalls")
    private Map<String, RecallConf> recalls;
    
    @JsonProperty("exposure_list")
    private String exposureList;
    
    @JsonProperty("context_params")
    private Map<String, Object> contextParams;
    
    @JsonProperty("debug")
    private boolean debug;
    
    public RecallRequest() {
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public String getInstanceId() {
        return instanceId;
    }
    
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }
    
    public String getService() {
        return service;
    }
    
    public void setService(String service) {
        this.service = service;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getUid() {
        return uid;
    }
    
    public void setUid(String uid) {
        this.uid = uid;
    }
    
    public Map<String, RecallConf> getRecalls() {
        return recalls;
    }
    
    public void setRecalls(Map<String, RecallConf> recalls) {
        this.recalls = recalls;
    }
    
    public String getExposureList() {
        return exposureList;
    }
    
    public void setExposureList(String exposureList) {
        this.exposureList = exposureList;
    }
    
    public Map<String, Object> getContextParams() {
        return contextParams;
    }
    
    public void setContextParams(Map<String, Object> contextParams) {
        this.contextParams = contextParams;
    }
    
    public boolean isDebug() {
        return debug;
    }
    
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
