package com.aliyun.openservices.pairec.api;

import com.aliyun.openservices.pairec.common.Constants;
import com.aliyun.openservices.pairec.model.ExperimentRoom;
import com.aliyun.pairecservice20221213.models.ListLaboratoriesRequest;
import com.aliyun.pairecservice20221213.models.ListLaboratoriesResponse;
import com.aliyun.pairecservice20221213.models.ListLaboratoriesResponseBody;
import com.aliyun.tea.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ExperimentRoomApi extends BaseApi {

    public ExperimentRoomApi(ApiClient apiClient) {
        super(apiClient);
    }

    public List<ExperimentRoom> listExperimentRooms(String environment, Long sceneId, Integer status) throws Exception {
        List<ExperimentRoom> experimentRooms = new ArrayList<>();
        ListLaboratoriesRequest listLaboratoriesRequest = new ListLaboratoriesRequest();
        listLaboratoriesRequest.setEnvironment(Constants.environmentDesc2OpenApiString(environment));
        listLaboratoriesRequest.setSceneId(String.valueOf(sceneId));
        listLaboratoriesRequest.setInstanceId(apiClient.getInstanceId());

        if (status == Constants.ExpRoom_Status_Online) {
            listLaboratoriesRequest.setStatus("Online");
        } else if (status == Constants.ExpRoom_Status_Offline) {
            listLaboratoriesRequest.setStatus("Offline");
        }

        ListLaboratoriesResponse listLaboratoriesResponse = apiClient.getClient().listLaboratories(listLaboratoriesRequest);

        for (ListLaboratoriesResponseBody.ListLaboratoriesResponseBodyLaboratories laboratory : listLaboratoriesResponse.getBody().getLaboratories()) {
            ExperimentRoom experimentRoom = new ExperimentRoom();
            experimentRoom.setExpRoomId(Long.valueOf(laboratory.getLaboratoryId()));
            experimentRoom.setExpRoomName(laboratory.getName());
            experimentRoom.setDebugUsers(laboratory.getDebugUsers());
            experimentRoom.setBucketCount(laboratory.getBucketCount());
            experimentRoom.setFilter(laboratory.getFilter());
            experimentRoom.setExpRoomBuckets(laboratory.getBuckets());
            experimentRoom.setDebugCrowdId(0);
            if (!StringUtils.isEmpty(laboratory.getDebugCrowdId())) {
                experimentRoom.setDebugCrowdId(Integer.valueOf(laboratory.getDebugCrowdId()));
            }
            experimentRoom.setSceneId(sceneId);
            if (laboratory.getType().equals("Base")) {
                experimentRoom.setType(Constants.ExpRoom_Type_Base);
            } else if (laboratory.getType().equals("NonBase")) {
                experimentRoom.setType(Constants.ExpRoom_Type_Normal);
            }

            if (laboratory.getBucketType().equals("Filter")) {
                experimentRoom.setBucketType(Constants.Bucket_Type_Filter);
            } else if (laboratory.getBucketType().equals("Uid")) {
                experimentRoom.setBucketType(Constants.Bucket_Type_UID);
            } else if (laboratory.getBucketType().equals("UidHash")) {
                experimentRoom.setBucketType(Constants.Bucket_Type_UID_HASH);
            } else {
                experimentRoom.setBucketType(Constants.Bucket_Type_Custom);
            }
            experimentRoom.setEnvironment(environment);

            experimentRooms.add(experimentRoom);
        }
        return experimentRooms;
    }

}
