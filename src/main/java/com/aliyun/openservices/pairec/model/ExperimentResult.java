package com.aliyun.openservices.pairec.model;

import com.aliyun.openservices.pairec.common.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExperimentResult {
    private String expId = "";

    private String sceneName;

    private ExperimentContext experimentContext = null;

    private ExperimentRoom experimentRoom = null;

    private List<Layer> layerList = new ArrayList<>();

    private Map<String, ExperimentGroup> layer2ExperimentGroup = new HashMap<>();

    private Map<String, Experiment> layer2Experiment = new HashMap<>();

    private Map<String, LayerParams> layerParamsMap = new HashMap<>();

    private LayerParams mergedLayerParams;

    public ExperimentResult(String sceneName, ExperimentContext experimentContext) {
        this.sceneName = sceneName;
        this.experimentContext = experimentContext;
    }

    public String getSceneName() {
        return sceneName;
    }

    public void setSceneName(String sceneName) {
        this.sceneName = sceneName;
    }

    public ExperimentContext getExperimentContext() {
        return experimentContext;
    }

    public void setExperimentContext(ExperimentContext experimentContext) {
        this.experimentContext = experimentContext;
    }

    public ExperimentRoom getExperimentRoom() {
        return experimentRoom;
    }

    public void setExperimentRoom(ExperimentRoom experimentRoom) {
        this.experimentRoom = experimentRoom;
    }

    public List<Layer> getLayerList() {
        return layerList;
    }

    public void setLayerList(List<Layer> layerList) {
        this.layerList = layerList;
    }

    public String getExpId() {
        return expId;
    }

    public void addMatchExperimentGroup(String layerName, ExperimentGroup experimentGroup) {
        this.layer2ExperimentGroup.put(layerName, experimentGroup);
    }

    public void addMatchExperiment(String layerName, Experiment experiment) {
        this.layer2Experiment.put(layerName, experiment);
    }

    public int layerSize() {
        return this.layerList.size();
    }

    public boolean containsLayer(String layerName) {
        for (Layer layer : this.layerList) {
            if (layer.getLayerName().equals(layerName)) {
                return true;
            }
        }

        return false;
    }

    public void init() {
        StringBuilder buf = new StringBuilder();
        if (null != this.experimentRoom) {
            buf.append("ER").append(this.experimentRoom.getExpRoomId());
        }

        for (Layer layer : this.layerList) {
            buf.append("_L").append(layer.getLayerId());

            if (this.layer2ExperimentGroup.containsKey(layer.getLayerName())) {
                buf.append("#");
                ExperimentGroup experimentGroup = this.layer2ExperimentGroup.get(layer.getLayerName());
                buf.append("EG").append(experimentGroup.getExpGroupId());

                LayerParams layerParams = new DefaultLayerParams();

                layerParams.addParams(experimentGroup.getExperimentParams());

                if (this.layer2Experiment.containsKey(layer.getLayerName())) {
                    Experiment experiment = this.layer2Experiment.get(layer.getLayerName());
                    if (experiment.getType() != Constants.Experiment_Type_Default) {
                        buf.append("#");
                        buf.append("E").append(experiment.getExperimentId());
                    }

                    layerParams.addParams(experiment.getExperimentParams());
                }

                this.layerParamsMap.put(layer.getLayerName(), layerParams);
            }
        }

        this.expId = buf.toString();
    }

    public LayerParams getLayerParams(String layerName) {
        if (null == this.experimentRoom || layerSize() == 0) {
            return new EmptyLayerParams();
        }

        if (layerSize() == 1) {
            if (this.layerParamsMap.containsKey(this.layerList.get(0).getLayerName())) {
                return this.layerParamsMap.get(this.layerList.get(0).getLayerName());
            }
        }

        if (this.layerParamsMap.containsKey(layerName)) {
            return this.layerParamsMap.get(layerName);
        }

        return new EmptyLayerParams();
    }

    public String info() {
        StringBuilder buf = new StringBuilder();

        if (null != this.experimentContext) {
            buf.append(",requestId=").append(this.experimentContext.getRequestId());
            buf.append(",uid=").append(this.experimentContext.getUid());
        }

        buf.append(",scene_name=").append(this.sceneName);

        if (null != this.experimentRoom) {
            buf.append(",exp_room_id=").append(this.experimentRoom.getExpRoomId());
        }

        buf.append(",exp_id=").append(this.expId);

        buf.deleteCharAt(0);

        return buf.toString();
    }

    public LayerParams getExperimentParams() {
        if (this.mergedLayerParams == null) {
            DefaultLayerParams mergedLayParams = new DefaultLayerParams();
            for (LayerParams value : this.layerParamsMap.values()) {
                if (value instanceof DefaultLayerParams) {
                    DefaultLayerParams params = (DefaultLayerParams) value;
                    for (String key : params.parameters.keySet()) {
                        mergedLayParams.parameters.put(key, params.parameters.get(key));
                    }
                }
            }
            this.mergedLayerParams = mergedLayParams;
        }

        return this.mergedLayerParams;
    }
}
