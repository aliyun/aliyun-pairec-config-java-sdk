package com.aliyun.openservices.pairec.recallengine;

/**
 * RecallEngine exception for recall and write operations
 */
public class RecallEngineException extends Exception {
    
    public RecallEngineException(String message) {
        super(message);
    }
    
    public RecallEngineException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public RecallEngineException(Throwable cause) {
        super(cause);
    }
}
