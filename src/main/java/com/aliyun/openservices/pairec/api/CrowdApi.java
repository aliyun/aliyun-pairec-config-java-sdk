
package com.aliyun.openservices.pairec.api;

import com.aliyun.pairecservice20221213.models.ListCrowdUsersRequest;
import com.aliyun.pairecservice20221213.models.ListCrowdUsersResponse;


import java.util.List;

public class CrowdApi extends  BaseApi{
    private ApiClient apiClient;

    public CrowdApi(ApiClient apiClient) {
        super(apiClient);
    }


    public List<String> listCrowdUsers(Long crowdId) throws Exception {
        ListCrowdUsersRequest listCrowdUsersRequest = new ListCrowdUsersRequest();
        listCrowdUsersRequest.setInstanceId(apiClient.getInstanceId());

        ListCrowdUsersResponse listCrowdUsersResponse = apiClient.getClient().listCrowdUsers(String.valueOf(crowdId), listCrowdUsersRequest);

        return listCrowdUsersResponse.getBody().getUsers();
    }
}
