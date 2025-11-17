package com.aliyun.openservices.pairec.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateUtils {
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    public static String executeTemplate(String template, Map<String,Object> params){
        if(template == null || params == null) return template;

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()){
            String key = matcher.group(1);
            Object value = params.get(key);

            String replacement;
            if (value == null){
                replacement = "${" + key +"}";
            }else if (value instanceof Map || value instanceof Iterable){
                replacement = "${" + key + "}";
            }else {
                replacement = value.toString();
            }

            matcher.appendReplacement(result,Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }
}
