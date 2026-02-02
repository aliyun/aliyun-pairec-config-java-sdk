package com.aliyun.openservices.pairec.recallengine;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * RecallConf represents recall configuration
 */
public class RecallConf {
    @JsonProperty("trigger")
    private String trigger;
    
    @JsonProperty("count")
    private int count;
    
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
}
