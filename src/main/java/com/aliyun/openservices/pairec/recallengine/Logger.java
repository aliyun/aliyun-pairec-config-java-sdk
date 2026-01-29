package com.aliyun.openservices.pairec.recallengine;

/**
 * Logger interface for RecallEngine client logging
 */
public interface Logger {
    /**
     * Log a formatted message
     * 
     * @param format the message format string
     * @param args the arguments to format into the message
     */
    void printf(String format, Object... args);
}
