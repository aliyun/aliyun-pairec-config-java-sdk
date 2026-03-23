package com.aliyun.openservices.pairec.recallengine;

/**
 * Callback interface for async write operations
 */
public interface WriteCallback {

    /**
     * Called when write operation succeeds
     *
     * @param instanceId the instance ID
     * @param table the table name
     * @param response the write response from server
     */
    void onSuccess(String instanceId, String table, WriteResponse response);

    /**
     * Called when write operation fails
     *
     * @param instanceId the instance ID
     * @param table the table name
     * @param e the exception that caused the failure
     */
    void onError(String instanceId, String table, Exception e);
}
