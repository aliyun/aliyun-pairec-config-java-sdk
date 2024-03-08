package com.aliyun.openservices.pairec;

import com.aliyun.openservices.pairec.api.ApiClient;
import com.aliyun.openservices.pairec.common.Constants;
import com.aliyun.openservices.pairec.model.DefaultSceneParams;
import com.aliyun.openservices.pairec.model.EmptySceneParams;
import com.aliyun.openservices.pairec.model.Experiment;
import com.aliyun.openservices.pairec.model.ExperimentContext;
import com.aliyun.openservices.pairec.model.ExperimentGroup;
import com.aliyun.openservices.pairec.model.ExperimentResult;
import com.aliyun.openservices.pairec.model.ExperimentRoom;
import com.aliyun.openservices.pairec.model.Layer;
import com.aliyun.openservices.pairec.model.Param;
import com.aliyun.openservices.pairec.model.Scene;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.aliyun.openservices.pairec.model.SceneParams;
import com.aliyun.openservices.pairec.util.FNV;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExperimentClient {
    public static Logger logger = LoggerFactory.getLogger(ExperimentClient.class);

    private ApiClient apiClient ;

    /**
     * cache experiment data by per scene
     */
    Map<String, Scene> sceneMap = new HashMap<>();

    /**
     * cache param data by per scene
     */
    Map<String, SceneParams> sceneParamData = new HashMap<>();

    ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(2);

    boolean started = false;

    public ExperimentClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    private static class ExperimentWorker extends Thread {

        ExperimentClient experimentClient;

        public ExperimentWorker(ExperimentClient client) {
            experimentClient = client;
            super.setDaemon(true);
        }

        @Override
        public void run() {
            try {
                experimentClient.loadExperimentData();
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
    }

    private static class SceneParamWorker extends Thread {

        ExperimentClient experimentClient;

        public SceneParamWorker(ExperimentClient client) {
            experimentClient = client;
            super.setDaemon(true);
        }

        @Override
        public void run() {
            try {
                experimentClient.loadSceneParamsData();
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
    }

    public synchronized void init() throws Exception {
        if (started) {
            return;
        }

        logger.debug("experiment client init");
        loadExperimentData();
        loadSceneParamsData();
        ExperimentWorker worker = new ExperimentWorker(this);
        scheduledThreadPool.scheduleWithFixedDelay(worker, 60, 60, TimeUnit.SECONDS);

        SceneParamWorker sceneParamWorker = new SceneParamWorker(this);
        scheduledThreadPool.scheduleWithFixedDelay(sceneParamWorker, 60, 60, TimeUnit.SECONDS);
        started = true;
    }


    /**
     * load experiment data from ab server
     *
     * @throws Exception
     */
    public void loadExperimentData() throws Exception {
        Map<String, Scene> sceneData = new HashMap<>();

        List<Scene> scenes= apiClient.getSceneApi().listAllScenes();

        for (Scene scene : scenes) {
            List<ExperimentRoom> experimentRooms = apiClient.getExperimentRoomApi().listExperimentRooms(apiClient.getConfiguration().getEnvironment(),
                    scene.getSceneId(), Constants.ExpRoom_Status_Online);

            for (ExperimentRoom experimentRoom : experimentRooms) {
                if (experimentRoom.getDebugCrowdId() != null && experimentRoom.getDebugCrowdId() > 0) {
                    List<String> expRoomDebugUsers = apiClient.getCrowdApi().listCrowdUsers(Long.valueOf(experimentRoom.getDebugCrowdId()));
                    experimentRoom.setDebugCrowdIdUsers(expRoomDebugUsers);
                }
                experimentRoom.init();
                scene.addExperimentRoom(experimentRoom);

                List<Layer>  layers = apiClient.getLayerApi().listLayers(experimentRoom.getExpRoomId());

                for (Layer layer : layers) {
                    experimentRoom.addLayer(layer);

                    List<ExperimentGroup> experimentGroups = apiClient.getExperimentGroupApi().listExperimentGroups(layer.getLayerId(),
                            experimentRoom.getExpRoomId(), Constants.ExpGroup_Status_Online);

                    for (ExperimentGroup experimentGroup : experimentGroups) {
                        if (experimentGroup.getCrowdId() != null && experimentGroup.getCrowdId() > 0) {
                            List<String> crowdUsers = apiClient.getCrowdApi().listCrowdUsers(experimentGroup.getCrowdId());
                            experimentGroup.setCrowdUsers(crowdUsers);
                        }
                        if (experimentGroup.getDebugCrowdId() != null && experimentGroup.getDebugCrowdId() > 0) {
                            List<String> debugCrowdUsers = apiClient.getCrowdApi().listCrowdUsers(Long.valueOf(experimentGroup.getDebugCrowdId()));
                            experimentGroup.setDebugCrowdUsers(debugCrowdUsers);
                        }

                        experimentGroup.init();
                        layer.addExperimentGroup(experimentGroup);

                        List<Experiment> experiments = apiClient.getExperimentApi().listExperiments(experimentGroup.getExpGroupId(),
                                Constants.Experiment_Status_Online);


                        for (Experiment experiment : experiments) {
                            if (experiment.getDebugCrowdId() != null && experiment.getDebugCrowdId() > 0) {
                                List<String> debugCrowdUsers = apiClient.getCrowdApi().listCrowdUsers(Long.valueOf(experiment.getDebugCrowdId()));
                                experiment.setDebugCrowdUsers(debugCrowdUsers);
                            }
                            experiment.init();
                            experimentGroup.addExperiment(experiment);
                        }
                    }
                }
            }

            sceneData.put(scene.getSceneName(), scene);
        }

        if (sceneData.size() > 0) {
            this.sceneMap = sceneData;
        }
    }

    public Map<String, Scene> getSceneMap() {
        return sceneMap;
    }

    /**
     * This method determines traffic based on the experimental context and returns the matching result
     *
     * @param sceneName scene name
     * @param experimentContext
     * @return ExperimentResult
     */
    public ExperimentResult matchExperiment(String sceneName, ExperimentContext experimentContext) {
        Map<String, Scene> sceneData = sceneMap;
        ExperimentResult experimentResult = new ExperimentResult(sceneName, experimentContext);

        if (!sceneData.containsKey(sceneName)) {
            logger.warn("scene:{}, not found the scene info", sceneName);
            return experimentResult;
        }

        Scene scene = sceneData.get(sceneName);
        ExperimentRoom defaultExperimentRoom = null;
        ExperimentRoom matchExperimentRoom = null;

        for (ExperimentRoom expRoom : scene.getExperimentRooms()) {
            if (expRoom.getType() == Constants.ExpRoom_Type_Base) {
                defaultExperimentRoom = expRoom;
            } else if (expRoom.matchDebugUsers(experimentContext)) {
                matchExperimentRoom = expRoom;
                break;
            } else if (expRoom.match(experimentContext)) {
                matchExperimentRoom = expRoom;
                break;
            }
        }

        if (null == matchExperimentRoom) {
            matchExperimentRoom = defaultExperimentRoom;
        }

        if (null != matchExperimentRoom) {
            experimentResult.setExperimentRoom(matchExperimentRoom);
            experimentResult.setLayerList(matchExperimentRoom.getLayerList());

            for (Layer layer : matchExperimentRoom.getLayerList()) {
                ExperimentGroup experimentGroup = layer.findMatchExperimentGroup(experimentContext);

                if (experimentGroup != null) {
                    experimentResult.addMatchExperimentGroup(layer.getLayerName(), experimentGroup);

                    Experiment defaultExperiment = null;
                    Experiment matchExperiment = null;

                    for (Experiment experiment : experimentGroup.getExperimentList()) {
                        if (experiment.getType() == Constants.Experiment_Type_Default) {
                            defaultExperiment = experiment;
                        }
                    }

                    // find match experiment
                    if (null == matchExperiment) {
                        // first find debug user experiment
                        for (Experiment experiment : experimentGroup.getExperimentList()) {
                            if (experiment.getType() != Constants.Experiment_Type_Default && experiment.matchDebugUser(experimentContext)) {
                                matchExperiment = experiment;
                                logger.debug("match experiment debug users uid:{}", experimentContext.getUid());
                                break;
                            }
                        }

                        if (null == matchExperiment) {
                            StringBuilder hashKey = new StringBuilder(experimentContext.getUid());
                            hashKey.append("_EXPROOM").append(experimentGroup.getExpRoomId())
                                    .append("_LAYER").append(experimentGroup.getLayerId())
                                    .append("_EXPGROUP").append(experimentGroup.getExpGroupId());

                            String hashValue = hashValue(hashKey.toString());
                            logger.debug("match experiment hash key:{}, value:{}", hashKey, hashValue);
                            experimentContext.setExperimentHashStr(hashValue);

                            for (Experiment experiment : experimentGroup.getExperimentList()) {
                                if (experiment.getType() != Constants.Experiment_Type_Default && experiment.match(experimentContext)) {
                                    matchExperiment = experiment;
                                    break;
                                }

                            }
                        }
                    }

                    if (null == matchExperiment) {
                        // if defaultExperiment not found ,set baseExperiment is  defaultExperiment
                        if (null == defaultExperiment) {
                            for (Experiment experiment : experimentGroup.getExperimentList()) {
                                if (experiment.getType() == Constants.Experiment_Type_Base) {
                                    defaultExperiment = experiment.cloneExperiment();
                                    defaultExperiment.setType(Constants.Experiment_Type_Default);
                                }
                            }
                        }
                        matchExperiment = defaultExperiment;
                    }

                    if (null != matchExperiment) {
                        experimentResult.addMatchExperiment(layer.getLayerName(), matchExperiment);
                    }
                }
            }
        }
        experimentResult.init();
        return experimentResult;
    }

    private String hashValue(String hashKey) {
        byte[] md5Hex = DigestUtils.md5(hashKey);
        BigInteger value = (new FNV()).fnv1_64(md5Hex);

        return value.toString();
    }

    public void close() throws Exception {
        scheduledThreadPool.awaitTermination(1, TimeUnit.SECONDS);
        scheduledThreadPool.shutdownNow();
        started = false;
    }

    /**
     * load param data from A/B Server
     */
    public void loadSceneParamsData() throws Exception {
        Map<String, SceneParams> sceneData = new HashMap<>();

        List<Scene>  scenes = apiClient.getSceneApi().listAllScenes();

        for (Scene scene : scenes) {
            SceneParams sceneParams = new DefaultSceneParams();
            List<Param> params = apiClient.getParamApi().getParam(scene.getSceneId(), apiClient.getConfiguration().getEnvironment(), null, null);

            for (Param param : params) {
                sceneParams.addParam(param.getParamName(), param.getParamValue());
            }

            sceneData.put(scene.getSceneName(), sceneParams);
        }

        if (sceneData.size() > 0) {
            this.sceneParamData = sceneData;
        }
    }

    /**
     * get scene params by sceneName, if not found scene params, return {@link EmptySceneParams}
     *
     * @param sceneName
     * @return
     */
    public SceneParams getSceneParams(String sceneName) {
        if (this.sceneParamData.containsKey(sceneName)) {
            return this.sceneParamData.get(sceneName);
        }

        return new EmptySceneParams();
    }
}
