package com.aliyun.openservices.pairec.model;

import java.util.Map;

public interface LayerParams {
    void addParam(String key, Object value);

    void addParams(Map<String, Object> params);

    Object get(String key, Object defaultValue);

    String getString(String key, String defaultValue);

    int getInt(String key, int defaultValue);

    long getLong(String key, long defaultValue);

    float getFloat(String key, float defaultValue);

    double getDouble(String key, double defaultValue);

    Map<String, Object>getAllParams();
}
