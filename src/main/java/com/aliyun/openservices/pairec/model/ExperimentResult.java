package com.aliyun.openservices.pairec.model;

import com.aliyun.openservices.pairec.common.Constants;
import com.aliyun.openservices.pairec.util.JsonUtils;
import com.aliyun.openservices.pairec.util.TemplateUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CheckedOutputStream;

import static com.aliyun.openservices.pairec.ExperimentClient.logger;

public class ExperimentResult {
    private static final Logger log = LoggerFactory.getLogger(ExperimentResult.class);
    private String expId = "";

    private String sceneName;

    private ExperimentContext experimentContext = null;

    private ExperimentRoom experimentRoom = null;

    private List<Layer> layerList = new ArrayList<>();

    private Map<String, ExperimentGroup> layer2ExperimentGroup = new HashMap<>();

    private Map<String, Experiment> layer2Experiment = new HashMap<>();

    private Map<String, LayerParams> layerParamsMap = new HashMap<>();

    private LayerParams mergedLayerParams;

    private ExperimentResult globalSceneExperimentResult;

    private Map<String,Object> globalParams = new HashMap<>();

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
        if (null != this.experimentRoom && !Constants.Global_Scene_Name.equals(sceneName)) {
            buf.append("ER").append(this.experimentRoom.getExpRoomId());
        }

        for(int i = 0;i<this.layerList.size();i++){
            Layer layer = this.layerList.get(i);

            if (Constants.Global_Scene_Name.equals(sceneName)){
                if(i == 0){
                    buf.append("GL");
                }else {
                    buf.append("_GL");
                }
            }else {
                buf.append("_L");
            }
            buf.append(layer.getLayerId());

            String layerName = layer.getLayerName();
            ExperimentGroup group = this.layer2ExperimentGroup.get(layerName);
            if (group == null){
                continue;
            }
            buf.append("#EG").append(group.getExpGroupId());

            LayerParams layerParams = new DefaultLayerParams();
            String groupConfig = group.getExpGroupConfig();

            if(groupConfig != null && !groupConfig.isEmpty()){
                if(Constants.Global_Scene_Name.equals(sceneName)){
                    try{
                        Map<String,Object> parsed = JsonUtils.parseJson(groupConfig);
                        this.globalParams.putAll(parsed);
                        if(this.experimentContext != null && this.experimentContext.getFilterParams() != null){
                            this.experimentContext.getFilterParams().putAll(parsed);
                        }
                    }catch (Exception e){
                        logger.error(e.getMessage());
                    }
                }else {
                    if(groupConfig.contains("${")&& this.globalSceneExperimentResult != null&& !this.globalSceneExperimentResult.getGlobalParams().isEmpty()){
                        groupConfig = TemplateUtils.executeTemplate(groupConfig,this.globalSceneExperimentResult.getGlobalParams());
                    }
                    try{
                        Map<String,Object> parsed = JsonUtils.parseJson(groupConfig);
                        layerParams.addParams(parsed);
                    }catch (Exception e){
                        logger.error(e.getMessage());
                    }
                }
            }
            Experiment experiment = this.layer2Experiment.get(layerName);
            if(experiment != null){
                if(experiment.getType() != Constants.Experiment_Type_Default){
                    buf.append("#E").append(experiment.getExperimentId());
                }

                String expConfig = experiment.getExperimentConfig();
                if(expConfig != null && !expConfig.isEmpty()){
                    if(Constants.Global_Scene_Name.equals(sceneName)){
                        try{
                            Map<String,Object> parsed = JsonUtils.parseJson(expConfig);
                            this.globalParams.putAll(parsed);
                            if(this.experimentContext!= null && this.experimentContext.getFilterParams() != null){
                                this.experimentContext.getFilterParams().putAll(parsed);
                            }
                        }catch (Exception e){
                            logger.error(e.getMessage());
                        }
                    }else {
                        if (expConfig.contains("${")&&this.globalSceneExperimentResult != null && !this.globalSceneExperimentResult.getGlobalParams().isEmpty()){
                            expConfig = TemplateUtils.executeTemplate(expConfig,this.globalSceneExperimentResult.getGlobalParams());
                        }
                        try {
                            Map<String,Object> parsed = JsonUtils.parseJson(expConfig);
                            layerParams.addParams(parsed);
                        }catch (Exception e){
                            logger.error(e.getMessage());
                        }
                    }
                }
            }

            this.layerParamsMap.put(layerName,layerParams);
        }
        String id = trimTrailingDelimiter(buf.toString());

        if (!Constants.Global_Scene_Name.equals(sceneName) && this.globalSceneExperimentResult != null){
            String globalId = this.globalSceneExperimentResult.getExpId();
            if (StringUtils.isNotEmpty(globalId)) {
                id = id + "_" + globalId;
                id = trimTrailingDelimiter(id);
            }
        }

        this.expId = id;
    }

    private String trimTrailingDelimiter(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        int len = s.length();
        char last = s.charAt(len - 1);
        if (last == '_' || last == '#') {
            return s.substring(0, len - 1);
        }
        return s;
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

    public ExperimentResult getGlobalSceneExperimentResult() {
        return globalSceneExperimentResult;
    }

    public void setGlobalSceneExperimentResult(ExperimentResult globalSceneExperimentResult) {
        this.globalSceneExperimentResult = globalSceneExperimentResult;
    }

    public Map<String, Object> getGlobalParams() {
        return globalParams; // 或返回 new HashMap<>(globalParams) 保证封装性
    }
}
