package com.aliyun.openservices.pairec.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public class JsonUtils {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static Map<String,Object> parseJson(String json) throws Exception{
        return mapper.readValue(json, new TypeReference<Map<String, Object>>() {
        });
    }
}
