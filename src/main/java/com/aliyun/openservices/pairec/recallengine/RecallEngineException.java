package com.aliyun.openservices.pairec.recallengine;

/**
 * RecallEngine exception for recall and write operations
 */
public class RecallEngineException extends Exception {

    /**
     * HTTP status code returned by the service, or {@link #NO_STATUS_CODE} when
     * the failure happened before a response was received (connect/read
     * timeout, DNS failure, serialization error, ...).
     */
    public static final int NO_STATUS_CODE = 0;

    private final int statusCode;

    public RecallEngineException(String message) {
        this(message, NO_STATUS_CODE);
    }

    public RecallEngineException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public RecallEngineException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = NO_STATUS_CODE;
    }

    public RecallEngineException(Throwable cause) {
        super(cause);
        this.statusCode = NO_STATUS_CODE;
    }

    /**
     * @return the HTTP status code, or {@link #NO_STATUS_CODE} if the request
     *         never produced a response
     */
    public int getStatusCode() {
        return statusCode;
    }
}
