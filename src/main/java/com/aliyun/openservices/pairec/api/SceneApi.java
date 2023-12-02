package com.aliyun.openservices.pairec.api;

import com.aliyun.openservices.pairec.model.Scene;
import com.aliyun.pairecservice20221213.models.ListScenesRequest;
import com.aliyun.pairecservice20221213.models.ListScenesResponse;
import com.aliyun.pairecservice20221213.models.ListScenesResponseBody;


import java.util.ArrayList;
import java.util.List;

public class SceneApi  extends  BaseApi{

    public SceneApi(ApiClient apiClient) {
        super(apiClient);
    }

    public List<Scene> listAllScenes() throws Exception {
        List<Scene> scenes = new ArrayList<>();
        ListScenesRequest listScenesRequest = new ListScenesRequest();
        listScenesRequest.setInstanceId(this.apiClient.getInstanceId());

        ListScenesResponse listScenesResponse = this.apiClient.getClient().listScenes(listScenesRequest);

        for (ListScenesResponseBody.ListScenesResponseBodyScenes sceneItem :  listScenesResponse.getBody().getScenes() ) {
            Scene scene = new Scene();
            scene.setSceneId(Long.valueOf(sceneItem.getSceneId()));
            scene.setSceneName(sceneItem.getName());
            scenes.add(scene);
        }
        return scenes;
    }

}
