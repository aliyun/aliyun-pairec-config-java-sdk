package com.aliyun.openservices.pairec.api;

import com.aliyun.openservices.pairec.common.Constants;
import com.aliyun.openservices.pairec.model.ExperimentGroup;
import com.aliyun.pairecservice20221213.models.ListExperimentGroupsRequest;
import com.aliyun.pairecservice20221213.models.ListExperimentGroupsResponse;
import com.aliyun.pairecservice20221213.models.ListExperimentGroupsResponseBody;
import com.aliyun.tea.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ExperimentGroupApi extends BaseApi {

    public ExperimentGroupApi(ApiClient apiClient) {
        super(apiClient);
    }

    public List<ExperimentGroup> listExperimentGroups(Long layerId, Long expRoomId, Integer status) throws Exception {
        List<ExperimentGroup> experimentGroups = new ArrayList<>();
        ListExperimentGroupsRequest listExperimentGroupsRequest = new ListExperimentGroupsRequest();
        listExperimentGroupsRequest.setInstanceId(apiClient.getInstanceId());
        listExperimentGroupsRequest.setLayerId(String.valueOf(layerId));
        if (status == Constants.ExpGroup_Status_Online) {
            listExperimentGroupsRequest.setStatus("Online");
        } else if (status == Constants.ExpGroup_Status_Offline) {
            listExperimentGroupsRequest.setStatus("Offline");
        }

        ListExperimentGroupsResponse listExperimentGroupsResponse = apiClient.getClient().listExperimentGroups(listExperimentGroupsRequest);

        for (ListExperimentGroupsResponseBody.ListExperimentGroupsResponseBodyExperimentGroups group: listExperimentGroupsResponse.getBody().getExperimentGroups()) {
            ExperimentGroup experimentGroup = new ExperimentGroup();
            experimentGroup.setExpGroupId(Long.valueOf(group.getExperimentGroupId()));
            experimentGroup.setExpGroupName(group.getName());
            experimentGroup.setLayerId(layerId);
            experimentGroup.setDebugUsers(group.getDebugUsers());
            experimentGroup.setFilter(group.getFilter());
            experimentGroup.setExpGroupConfig(group.getConfig());
            experimentGroup.setReserveBuckets(group.getReservedBuckets());

            experimentGroup.setDebugCrowdId(0);
            if (!StringUtils.isEmpty(group.getDebugCrowdId())) {
                experimentGroup.setDebugCrowdId(Integer.valueOf(group.getDebugCrowdId()));
            }

            experimentGroup.setSceneId(Long.valueOf(group.getSceneId()));
            experimentGroup.setExpRoomId(expRoomId);
            if (group.getDistributionType().equals("UserId")) {
                experimentGroup.setDistributionType(Constants.ExpGroup_Distribution_Type_User);
            } else if (group.getDistributionType().equals("TimeDuration")) {
                experimentGroup.setDistributionType(Constants.ExpGroup_Distribution_Type_TimeDuration);
                experimentGroup.setDistributionTimeDuration(group.getDistributionTimeDuration());
            }
            if (!StringUtils.isEmpty(group.getCrowdId())) {
                experimentGroup.setCrowdId(Long.valueOf(group.getCrowdId()));
            }

            experimentGroups.add(experimentGroup);
        }
        return experimentGroups;
    }
}
