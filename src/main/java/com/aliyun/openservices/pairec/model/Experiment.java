package com.aliyun.openservices.pairec.model;

import com.aliyun.openservices.pairec.JSON;
import com.aliyun.tea.utils.StringUtils;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Experiment
 */

public class Experiment {
    @SerializedName("experiment_id")
    private Long experimentId = null;

    @SerializedName("exp_group_id")
    private Long expGroupId = null;

    @SerializedName("layer_id")
    private Long layerId = null;

    @SerializedName("exp_room_id")
    private Long expRoomId = null;

    @SerializedName("scene_id")
    private Long sceneId = null;

    @SerializedName("experiment_name")
    private String experimentName = null;

    @SerializedName("experiment_info")
    private String experimentInfo = null;

    @SerializedName("type")
    private Integer type = null;

    @SerializedName("experiment_flow")
    private Integer experimentFlow = null;

    @SerializedName("experiment_buckets")
    private String experimentBuckets = null;

    @SerializedName("debug_users")
    private String debugUsers = null;

    private Integer debugCrowdId = null;

    private List<String> debugCrowdUsers = null;
    @SerializedName("experiment_config")
    private String experimentConfig = null;

    @SerializedName("status")
    private Integer status = null;

    private final Map<String, Boolean> debugUserMap = new HashMap<>();

    DiversionBucket diversionBucket = null;

    private Map<String, Object> experimentParams = new HashMap<>();

    private final JSON json = new JSON();

    public Experiment experimentId(Long experimentId) {
        this.experimentId = experimentId;
        return this;
    }

    /**
     * Get experimentId
     *
     * @return experimentId
     **/
    public Long getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(Long experimentId) {
        this.experimentId = experimentId;
    }

    public Experiment expGroupId(Long expGroupId) {
        this.expGroupId = expGroupId;
        return this;
    }

    /**
     * Get expGroupId
     *
     * @return expGroupId
     **/
    public Long getExpGroupId() {
        return expGroupId;
    }

    public void setExpGroupId(Long expGroupId) {
        this.expGroupId = expGroupId;
    }

    public Experiment layerId(Long layerId) {
        this.layerId = layerId;
        return this;
    }

    /**
     * Get layerId
     *
     * @return layerId
     **/
    public Long getLayerId() {
        return layerId;
    }

    public void setLayerId(Long layerId) {
        this.layerId = layerId;
    }

    public Experiment expRoomId(Long expRoomId) {
        this.expRoomId = expRoomId;
        return this;
    }

    /**
     * Get expRoomId
     *
     * @return expRoomId
     **/
    public Long getExpRoomId() {
        return expRoomId;
    }

    public void setExpRoomId(Long expRoomId) {
        this.expRoomId = expRoomId;
    }

    public Experiment sceneId(Long sceneId) {
        this.sceneId = sceneId;
        return this;
    }

    /**
     * Get sceneId
     *
     * @return sceneId
     **/
    public Long getSceneId() {
        return sceneId;
    }

    public void setSceneId(Long sceneId) {
        this.sceneId = sceneId;
    }

    public Experiment experimentName(String experimentName) {
        this.experimentName = experimentName;
        return this;
    }

    /**
     * Get experimentName
     *
     * @return experimentName
     **/
    public String getExperimentName() {
        return experimentName;
    }

    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
    }

    public Experiment experimentInfo(String experimentInfo) {
        this.experimentInfo = experimentInfo;
        return this;
    }

    /**
     * Get experimentInfo
     *
     * @return experimentInfo
     **/
    public String getExperimentInfo() {
        return experimentInfo;
    }

    public void setExperimentInfo(String experimentInfo) {
        this.experimentInfo = experimentInfo;
    }

    public Experiment type(Integer type) {
        this.type = type;
        return this;
    }

    /**
     * Get type
     *
     * @return type
     **/
    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Experiment experimentFlow(Integer experimentFlow) {
        this.experimentFlow = experimentFlow;
        return this;
    }

    /**
     * Get experimentFlow
     *
     * @return experimentFlow
     **/
    public Integer getExperimentFlow() {
        return experimentFlow;
    }

    public void setExperimentFlow(Integer experimentFlow) {
        this.experimentFlow = experimentFlow;
    }

    public Experiment experimentBuckets(String experimentBuckets) {
        this.experimentBuckets = experimentBuckets;
        return this;
    }

    /**
     * Get experimentBuckets
     *
     * @return experimentBuckets
     **/
    public String getExperimentBuckets() {
        return experimentBuckets;
    }

    public void setExperimentBuckets(String experimentBuckets) {
        this.experimentBuckets = experimentBuckets;
    }

    public Experiment debugUsers(String debugUsers) {
        this.debugUsers = debugUsers;
        return this;
    }

    /**
     * Get debugUsers
     *
     * @return debugUsers
     **/
    public String getDebugUsers() {
        return debugUsers;
    }

    public void setDebugUsers(String debugUsers) {
        this.debugUsers = debugUsers;
    }

    public Experiment experimentConfig(String experimentConfig) {
        this.experimentConfig = experimentConfig;
        return this;
    }

    public Integer getDebugCrowdId() {
        return debugCrowdId;
    }

    public void setDebugCrowdId(Integer debugCrowdId) {
        this.debugCrowdId = debugCrowdId;
    }

    public List<String> getDebugCrowdUsers() {
        return debugCrowdUsers;
    }

    public void setDebugCrowdUsers(List<String> debugCrowdUsers) {
        this.debugCrowdUsers = debugCrowdUsers;
    }

    /**
     * Get experimentConfig
     *
     * @return experimentConfig
     **/
    public String getExperimentConfig() {
        return experimentConfig;
    }

    public void setExperimentConfig(String experimentConfig) {
        this.experimentConfig = experimentConfig;
    }

    public Experiment status(Integer status) {
        this.status = status;
        return this;
    }

    /**
     * Get status
     *
     * @return status
     **/
    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Experiment experiment = (Experiment) o;
        return Objects.equals(this.experimentId, experiment.experimentId) &&
                Objects.equals(this.expGroupId, experiment.expGroupId) &&
                Objects.equals(this.layerId, experiment.layerId) &&
                Objects.equals(this.expRoomId, experiment.expRoomId) &&
                Objects.equals(this.sceneId, experiment.sceneId) &&
                Objects.equals(this.experimentName, experiment.experimentName) &&
                Objects.equals(this.experimentInfo, experiment.experimentInfo) &&
                Objects.equals(this.type, experiment.type) &&
                Objects.equals(this.experimentFlow, experiment.experimentFlow) &&
                Objects.equals(this.experimentBuckets, experiment.experimentBuckets) &&
                Objects.equals(this.debugUsers, experiment.debugUsers) &&
                Objects.equals(this.experimentConfig, experiment.experimentConfig) &&
                Objects.equals(this.debugCrowdId, experiment.debugCrowdId) &&
                Objects.equals(this.debugCrowdUsers, experiment.debugCrowdUsers) &&
                Objects.equals(this.status, experiment.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(experimentId, expGroupId, layerId, expRoomId, sceneId, experimentName, experimentInfo, type, experimentFlow, experimentBuckets, debugUsers, experimentConfig, status, debugCrowdId, debugCrowdUsers);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Experiment {\n");

        sb.append("    experimentId: ").append(toIndentedString(experimentId)).append("\n");
        sb.append("    expGroupId: ").append(toIndentedString(expGroupId)).append("\n");
        sb.append("    layerId: ").append(toIndentedString(layerId)).append("\n");
        sb.append("    expRoomId: ").append(toIndentedString(expRoomId)).append("\n");
        sb.append("    sceneId: ").append(toIndentedString(sceneId)).append("\n");
        sb.append("    experimentName: ").append(toIndentedString(experimentName)).append("\n");
        sb.append("    experimentInfo: ").append(toIndentedString(experimentInfo)).append("\n");
        sb.append("    type: ").append(toIndentedString(type)).append("\n");
        sb.append("    experimentFlow: ").append(toIndentedString(experimentFlow)).append("\n");
        sb.append("    experimentBuckets: ").append(toIndentedString(experimentBuckets)).append("\n");
        sb.append("    debugUsers: ").append(toIndentedString(debugUsers)).append("\n");
        sb.append("    experimentConfig: ").append(toIndentedString(experimentConfig)).append("\n");
        sb.append("    status: ").append(toIndentedString(status)).append("\n");
        sb.append("    debugCrowdId: ").append(toIndentedString(debugCrowdId)).append("\n");
        sb.append("    debugCrowdUsers: ").append(toIndentedString(debugCrowdUsers)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }

    public void init() {
        if (!StringUtils.isEmpty(this.debugUsers)) {
            String[] users = this.debugUsers.split(",");
            for (String user : users) {
                this.debugUserMap.put(user, true);
            }
        }
        if (this.debugCrowdUsers != null && this.debugCrowdUsers.size() > 0) {
            for (String user: debugCrowdUsers) {
                this.debugUserMap.put(user, true);
            }
        }

        if (experimentFlow > 0 && experimentFlow < 100) {
            diversionBucket = new UidDiversionBucket(100, experimentBuckets);
        }

        if (!StringUtils.isEmpty(this.experimentConfig)) {
            this.experimentParams = json.deserialize(this.experimentConfig, this.experimentParams.getClass());
        }
    }

    public boolean matchDebugUser(ExperimentContext experimentContext) {
        return this.debugUserMap.containsKey(experimentContext.getUid());
    }

    public boolean match(ExperimentContext experimentContext) {
        if (experimentFlow == 0) {
            return false;
        }

        if (experimentFlow == 100) {
            return true;
        }

        if (debugUserMap.containsKey(experimentContext.getUid())) {
            return true;
        }

        if (null != this.diversionBucket) {
            ExperimentContext context = new ExperimentContext();
            context.setUid(experimentContext.getExperimentHashStr());
            return this.diversionBucket.match(context);
        }

        return false;
    }

    public Map<String, Object> getExperimentParams() {
        return experimentParams;
    }

    public Experiment cloneExperiment() {
        Experiment experiment = new Experiment();
        experiment.setExperimentId(this.getExperimentId());
        experiment.setExpRoomId(this.getExpRoomId());
        experiment.setSceneId(this.getSceneId());
        experiment.setLayerId(this.getLayerId());
        experiment.setStatus(this.getStatus());
        experiment.setExperimentConfig(this.getExperimentConfig());
        experiment.setExperimentFlow(this.getExperimentFlow());
        experiment.setDebugCrowdId(this.getDebugCrowdId());
        experiment.setDebugCrowdUsers(this.getDebugCrowdUsers());
        experiment.init();
        return experiment;
    }
}
