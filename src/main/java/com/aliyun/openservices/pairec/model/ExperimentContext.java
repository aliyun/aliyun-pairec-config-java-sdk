package com.aliyun.openservices.pairec.model;

import java.util.HashMap;
import java.util.Map;

public class ExperimentContext {
    private String requestId;

    private String Uid;

    private Map<String, Object> filterParams = new HashMap<>();
    private String experimentHashStr ;
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getUid() {
        return Uid;
    }

    public void setUid(String uid) {
        Uid = uid;
    }

    public Map<String, Object> getFilterParams() {
        return filterParams;
    }

    public void setFilterParams(Map<String, Object> filterParams) {
        this.filterParams = filterParams;
    }

    public void addFilterParam(String key, Object value) {
        this.filterParams.put(key, value);
    }

    public String getExperimentHashStr() {
        return experimentHashStr;
    }

    public void setExperimentHashStr(String experimentHashStr) {
        this.experimentHashStr = experimentHashStr;
    }
}
