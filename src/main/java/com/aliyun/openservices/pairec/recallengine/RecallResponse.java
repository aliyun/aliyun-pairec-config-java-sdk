package com.aliyun.openservices.pairec.recallengine;

/**
 * RecallResponse represents the response from a recall request
 */
public class RecallResponse {
    private Record result;
    
    public RecallResponse() {
    }
    
    public RecallResponse(Record result) {
        this.result = result;
    }
    
    public Record getResult() {
        return result;
    }
    
    public void setResult(Record result) {
        this.result = result;
    }
}
