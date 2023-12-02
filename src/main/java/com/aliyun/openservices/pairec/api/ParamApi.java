package com.aliyun.openservices.pairec.api;

import com.aliyun.openservices.pairec.common.Constants;
import com.aliyun.openservices.pairec.model.Param;
import com.aliyun.pairecservice20221213.models.ListParamsRequest;
import com.aliyun.pairecservice20221213.models.ListParamsResponse;
import com.aliyun.pairecservice20221213.models.ListParamsResponseBody;

import java.util.ArrayList;
import java.util.List;

public class ParamApi extends BaseApi {

    public ParamApi(ApiClient apiClient) {
        super(apiClient);
    }

    public List<Param> getParam(Long sceneId, String environment, Long paramId, String paramName) throws Exception {
        List<Param> paramList = new ArrayList<>();

        ListParamsRequest listParamsRequest = new ListParamsRequest();
        listParamsRequest.setInstanceId(apiClient.getInstanceId());
        listParamsRequest.setSceneId(String.valueOf(sceneId));
        listParamsRequest.setEnvironment(Constants.environmentDesc2OpenApiString(environment));

        ListParamsResponse listParamsResponse = apiClient.getClient().listParams(listParamsRequest);

        for (ListParamsResponseBody.ListParamsResponseBodyParams paramItem : listParamsResponse.getBody().getParams()) {
            Param param = new Param();
            param.setParamId(Long.valueOf(paramItem.getParamId()));
            param.setParamName(paramItem.getName());
            param.setParamValue(paramItem.getValue());
            param.setEnvironment(environment);
            paramList.add(param);
        }
        return paramList;
    }
}
