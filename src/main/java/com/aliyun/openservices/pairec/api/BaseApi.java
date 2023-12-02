package com.aliyun.openservices.pairec.api;

public class BaseApi {
    protected ApiClient apiClient;

    public BaseApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }
}
