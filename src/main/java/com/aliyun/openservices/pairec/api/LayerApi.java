package com.aliyun.openservices.pairec.api;

import com.aliyun.openservices.pairec.model.Layer;
import com.aliyun.pairecservice20221213.models.ListLayersRequest;
import com.aliyun.pairecservice20221213.models.ListLayersResponse;
import com.aliyun.pairecservice20221213.models.ListLayersResponseBody;

import java.util.ArrayList;
import java.util.List;

public class LayerApi extends BaseApi {

    public LayerApi(ApiClient apiClient) {
        super(apiClient);
    }


    public List<Layer> listLayers(Long expRoomId) throws Exception {
        List<Layer> layers = new ArrayList<>();
        ListLayersRequest listLayersRequest = new ListLayersRequest();
        listLayersRequest.setInstanceId(apiClient.getInstanceId());
        listLayersRequest.setLaboratoryId(String.valueOf(expRoomId));
        ListLayersResponse listLayersResponse = apiClient.getClient().listLayers(listLayersRequest);

        for (ListLayersResponseBody.ListLayersResponseBodyLayers layerItem : listLayersResponse.getBody().getLayers()) {
            Layer layer = new Layer();
            layer.setExpRoomId(expRoomId);
            layer.setLayerId(Long.valueOf(layerItem.getLayerId()));
            layer.setLayerName(layerItem.getName());
            layer.setSceneId(Long.valueOf(layerItem.getSceneId()));

            layers.add(layer);
        }

        return layers;
    }

}
