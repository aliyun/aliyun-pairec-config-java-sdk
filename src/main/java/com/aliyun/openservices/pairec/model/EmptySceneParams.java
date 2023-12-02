package com.aliyun.openservices.pairec.model;

import java.util.Map;

public class EmptySceneParams implements SceneParams{
    @Override
    public void addParam(String key, Object value) {

    }

    @Override
    public void addParams(Map<String, Object> params) {

    }

    @Override
    public Object get(String key, Object defaultValue) {
        return defaultValue;
    }

    @Override
    public String getString(String key, String defaultValue) {
        return defaultValue;
    }

    @Override
    public int getInt(String key, int defaultValue) {
        return defaultValue;
    }

    @Override
    public long getLong(String key, long defaultValue) {
        return defaultValue;
    }

    @Override
    public float getFloat(String key, float defaultValue) {
        return defaultValue;
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        return defaultValue;
    }

}
