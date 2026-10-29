package com.aliyun.openservices.pairec.recallengine;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Optional parameters for one recall way. Zero values are omitted.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class RecallOptions {
    @JsonProperty("trigger_limit")
    private int triggerLimit;

    @JsonProperty("timeout")
    private int timeout;

    public RecallOptions() {
    }

    public int getTriggerLimit() {
        return triggerLimit;
    }

    /**
     * Set the per-trigger X2I recall limit. Zero means no limit.
     *
     * @param triggerLimit per-trigger recall limit
     */
    public void setTriggerLimit(int triggerLimit) {
        this.triggerLimit = triggerLimit;
    }

    public int getTimeout() {
        return timeout;
    }

    /**
     * Set the server-side timeout for this recall way, in milliseconds.
     * Zero means no per-way timeout. This does not change the HTTP client timeout.
     *
     * @param timeout per-way timeout in milliseconds
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
