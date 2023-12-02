package com.aliyun.openservices.pairec.api;

import com.aliyun.openservices.pairec.common.Constants;
import com.aliyun.openservices.pairec.model.Experiment;
import com.aliyun.pairecservice20221213.models.ListExperimentsRequest;
import com.aliyun.pairecservice20221213.models.ListExperimentsResponse;
import com.aliyun.pairecservice20221213.models.ListExperimentsResponseBody;
import com.aliyun.tea.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ExperimentApi extends BaseApi {

    public ExperimentApi(ApiClient apiClient) {
        super(apiClient);
    }

    public List<Experiment> listExperiments(Long expGroupId, Integer status) throws Exception {
        List<Experiment> experiments = new ArrayList<>();

        ListExperimentsRequest listExperimentsRequest = new ListExperimentsRequest();
        listExperimentsRequest.setExperimentGroupId(String.valueOf(expGroupId));
        listExperimentsRequest.setInstanceId(apiClient.getInstanceId());
        if (status == Constants.Experiment_Status_Online) {
            listExperimentsRequest.setStatus("Online");
        } else if (status == Constants.Experiment_Status_Offline) {
            listExperimentsRequest.setStatus("Offline");
        }

        ListExperimentsResponse listExperimentsResponse = apiClient.getClient().listExperiments(listExperimentsRequest);
        for (ListExperimentsResponseBody.ListExperimentsResponseBodyExperiments exp : listExperimentsResponse.getBody().getExperiments()) {
            Experiment experiment = new Experiment();
            experiment.setExperimentId(Long.valueOf(exp.getExperimentId()));
            experiment.setSceneId(Long.valueOf(exp.getSceneId()));
            experiment.setLayerId(Long.valueOf(exp.getLayerId()));
            experiment.setExpGroupId(Long.valueOf(exp.getExperimentGroupId()));
            experiment.setExpRoomId(Long.valueOf(exp.getLaboratoryId()));
            experiment.setExperimentName(exp.getName());
            experiment.setExperimentFlow(exp.getFlowPercent());
            experiment.setExperimentBuckets(exp.getBuckets());
            experiment.setDebugUsers(exp.getDebugUsers());
            experiment.setExperimentConfig(exp.getConfig());
            if (!StringUtils.isEmpty(exp.getDebugCrowdId())) {
                experiment.setDebugCrowdId(Integer.valueOf(exp.getDebugCrowdId()));
            }

            if (exp.getType().equals("Baseline")) {
                experiment.setType(Constants.Experiment_Type_Base);
            } else if (exp.getType().equals("Normal")) {
                experiment.setType(Constants.Experiment_Type_Test);
            }
            experiments.add(experiment);
        }
        return experiments;
    }
}
