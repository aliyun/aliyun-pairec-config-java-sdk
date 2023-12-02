package com.aliyun.openservices.pairec.api;


import com.aliyun.pairecservice20221213.Client;

public class ApiClient {
    private Configuration configuration;
    Client client;

    private CrowdApi crowdApi;

    private SceneApi sceneApi;

    private ExperimentRoomApi experimentRoomApi;

    private LayerApi layerApi;
    private ExperimentGroupApi experimentGroupApi ;

    private ExperimentApi experimentApi ;

    private ParamApi paramApi ;


    public ApiClient(Configuration configuration) throws Exception {
        this.configuration = configuration;
        this.client = new Client(configuration.getConfig());

        this.sceneApi = new SceneApi(this);
        this.experimentRoomApi = new ExperimentRoomApi(this);
        this.layerApi = new LayerApi(this);
        this.experimentGroupApi = new ExperimentGroupApi(this);
        this.experimentApi = new ExperimentApi(this);
        this.paramApi = new ParamApi(this);
        this.crowdApi = new CrowdApi(this);
    }

    public Client getClient() {
        return client;
    }

    public String getInstanceId() {
        return this.configuration.getInstanceId();
    }

    public CrowdApi getCrowdApi() {
        return crowdApi;
    }

    public SceneApi getSceneApi() {
        return sceneApi;
    }

    public ExperimentRoomApi getExperimentRoomApi() {
        return experimentRoomApi;
    }

    public LayerApi getLayerApi() {
        return layerApi;
    }

    public ExperimentGroupApi getExperimentGroupApi() {
        return experimentGroupApi;
    }

    public ExperimentApi getExperimentApi() {
        return experimentApi;
    }

    public ParamApi getParamApi() {
        return paramApi;
    }

    public Configuration getConfiguration() {
        return configuration;
    }
}