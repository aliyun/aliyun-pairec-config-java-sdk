package com.aliyun.openservices.pairec.model;

import com.aliyun.openservices.pairec.JSON;
import com.aliyun.openservices.pairec.common.Constants;
import com.aliyun.tea.utils.StringUtils;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ExperimentGroup
 */

public class ExperimentGroup {
    @SerializedName("exp_group_id")
    private Long expGroupId = null;

    @SerializedName("layer_id")
    private Long layerId = null;

    @SerializedName("exp_room_id")
    private Long expRoomId = null;

    @SerializedName("scene_id")
    private Long sceneId = null;

    @SerializedName("exp_group_name")
    private String expGroupName = null;

    @SerializedName("exp_group_info")
    private String expGroupInfo = null;

    @SerializedName("debug_users")
    private String debugUsers = null;

    private Integer debugCrowdId = null;
    @SerializedName("owner")
    private String owner = null;

    @SerializedName("needAA")
    private Integer needAA = null;

    @SerializedName("filter")
    private String filter = null;

    @SerializedName("crowd_id")
    private Long crowdId = null;

    @SerializedName("exp_group_config")
    private String expGroupConfig = null;

    @SerializedName("reserve_buckets")
    private String reserveBuckets = null;

    @SerializedName("type")
    private Integer type = null;

    @SerializedName("status")
    private Integer status = null;

    private Integer distributionType = null;

    private Integer distributionTimeDuration = null;

    @SerializedName("crowd_target_type")
    private String crowdTargetType = null;

    @SerializedName("holding_buckets")
    private String holdingBuckets = null;
    private List<String> crowdUsers = null;

    private List<String> debugCrowdUsers = null;
    private final Map<String, Boolean> debugUserMap = new HashMap<>();


    private DiversionBucket diversionBucket = null;

    private final List<Experiment> experimentList = new ArrayList<>();

    private Map<String, Object> experimentParams = new HashMap<>();

    private final JSON json = new JSON();

    public ExperimentGroup expGroupId(Long expGroupId) {
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

    public ExperimentGroup layerId(Long layerId) {
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

    public ExperimentGroup expRoomId(Long expRoomId) {
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

    public ExperimentGroup sceneId(Long sceneId) {
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

    public ExperimentGroup expGroupName(String expGroupName) {
        this.expGroupName = expGroupName;
        return this;
    }

    /**
     * Get expGroupName
     *
     * @return expGroupName
     **/
    public String getExpGroupName() {
        return expGroupName;
    }

    public void setExpGroupName(String expGroupName) {
        this.expGroupName = expGroupName;
    }

    public ExperimentGroup expGroupInfo(String expGroupInfo) {
        this.expGroupInfo = expGroupInfo;
        return this;
    }

    /**
     * Get expGroupInfo
     *
     * @return expGroupInfo
     **/
    public String getExpGroupInfo() {
        return expGroupInfo;
    }

    public void setExpGroupInfo(String expGroupInfo) {
        this.expGroupInfo = expGroupInfo;
    }

    public ExperimentGroup debugUsers(String debugUsers) {
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

    public ExperimentGroup owner(String owner) {
        this.owner = owner;
        return this;
    }

    /**
     * Get owner
     *
     * @return owner
     **/
    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public ExperimentGroup needAA(Integer needAA) {
        this.needAA = needAA;
        return this;
    }

    /**
     * Get needAA
     *
     * @return needAA
     **/
    public Integer getNeedAA() {
        return needAA;
    }

    public void setNeedAA(Integer needAA) {
        this.needAA = needAA;
    }

    public ExperimentGroup filter(String filter) {
        this.filter = filter;
        return this;
    }

    /**
     * Get filter
     *
     * @return filter
     **/
    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    /**
     * Get crowdId
     *
     * @return crowdId
     */
    public Long getCrowdId() {
        return crowdId;
    }

    public void setCrowdId(Long crowdId) {
        this.crowdId = crowdId;
    }

    public ExperimentGroup expGroupConfig(String expGroupConfig) {
        this.expGroupConfig = expGroupConfig;
        return this;
    }

    /**
     * Get expGroupConfig
     *
     * @return expGroupConfig
     **/
    public String getExpGroupConfig() {
        return expGroupConfig;
    }

    public void setExpGroupConfig(String expGroupConfig) {
        this.expGroupConfig = expGroupConfig;
    }

    public ExperimentGroup reserveBuckets(String reserveBuckets) {
        this.reserveBuckets = reserveBuckets;
        return this;
    }

    /**
     * Get reserveBuckets
     *
     * @return reserveBuckets
     **/
    public String getReserveBuckets() {
        return reserveBuckets;
    }

    public void setReserveBuckets(String reserveBuckets) {
        this.reserveBuckets = reserveBuckets;
    }

    public ExperimentGroup type(Integer type) {
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

    public ExperimentGroup status(Integer status) {
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

    public List<String> getCrowdUsers() {
        return crowdUsers;
    }

    public void setCrowdUsers(List<String> crowdUsers) {
        this.crowdUsers = crowdUsers;
    }

    public Integer getDistributionType() {
        return distributionType;
    }

    public Integer getDistributionTimeDuration() {
        return distributionTimeDuration;
    }

    public void setDistributionType(Integer distributionType) {
        this.distributionType = distributionType;
    }

    public void setDistributionTimeDuration(Integer distributionTimeDuration) {
        this.distributionTimeDuration = distributionTimeDuration;
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

    public String getCrowdTargetType() {
        return crowdTargetType;
    }

    public void setCrowdTargetType(String crowdTargetType) {
        this.crowdTargetType = crowdTargetType;
    }

    public String getHoldingBuckets() {
        return holdingBuckets;
    }

    public void setHoldingBuckets(String holdingBuckets) {
        this.holdingBuckets = holdingBuckets;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExperimentGroup experimentGroup = (ExperimentGroup) o;
        return Objects.equals(this.expGroupId, experimentGroup.expGroupId) &&
                Objects.equals(this.layerId, experimentGroup.layerId) &&
                Objects.equals(this.expRoomId, experimentGroup.expRoomId) &&
                Objects.equals(this.sceneId, experimentGroup.sceneId) &&
                Objects.equals(this.expGroupName, experimentGroup.expGroupName) &&
                Objects.equals(this.expGroupInfo, experimentGroup.expGroupInfo) &&
                Objects.equals(this.debugUsers, experimentGroup.debugUsers) &&
                Objects.equals(this.owner, experimentGroup.owner) &&
                Objects.equals(this.needAA, experimentGroup.needAA) &&
                Objects.equals(this.filter, experimentGroup.filter) &&
                Objects.equals(this.expGroupConfig, experimentGroup.expGroupConfig) &&
                Objects.equals(this.reserveBuckets, experimentGroup.reserveBuckets) &&
                Objects.equals(this.type, experimentGroup.type) &&
                Objects.equals(this.distributionType, experimentGroup.distributionType) &&
                Objects.equals(this.distributionTimeDuration, experimentGroup.distributionTimeDuration) &&
                Objects.equals(this.debugCrowdId, experimentGroup.debugCrowdId) &&
                Objects.equals(this.crowdTargetType, experimentGroup.crowdTargetType) &&
                Objects.equals(this.holdingBuckets, experimentGroup.holdingBuckets) &&
                Objects.equals(this.status, experimentGroup.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expGroupId, layerId, expRoomId, sceneId, expGroupName, expGroupInfo, debugUsers, owner, needAA, filter, expGroupConfig, reserveBuckets, type, status,
                distributionType, distributionTimeDuration, debugCrowdId, crowdTargetType, holdingBuckets);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ExperimentGroup {\n");

        sb.append("    expGroupId: ").append(toIndentedString(expGroupId)).append("\n");
        sb.append("    layerId: ").append(toIndentedString(layerId)).append("\n");
        sb.append("    expRoomId: ").append(toIndentedString(expRoomId)).append("\n");
        sb.append("    sceneId: ").append(toIndentedString(sceneId)).append("\n");
        sb.append("    expGroupName: ").append(toIndentedString(expGroupName)).append("\n");
        sb.append("    expGroupInfo: ").append(toIndentedString(expGroupInfo)).append("\n");
        sb.append("    debugUsers: ").append(toIndentedString(debugUsers)).append("\n");
        sb.append("    owner: ").append(toIndentedString(owner)).append("\n");
        sb.append("    needAA: ").append(toIndentedString(needAA)).append("\n");
        sb.append("    filter: ").append(toIndentedString(filter)).append("\n");
        sb.append("    expGroupConfig: ").append(toIndentedString(expGroupConfig)).append("\n");
        sb.append("    reserveBuckets: ").append(toIndentedString(reserveBuckets)).append("\n");
        sb.append("    type: ").append(toIndentedString(type)).append("\n");
        sb.append("    status: ").append(toIndentedString(status)).append("\n");
        sb.append("    distributionType: ").append(toIndentedString(distributionType)).append("\n");
        sb.append("    distributionTimeDuration: ").append(toIndentedString(distributionTimeDuration)).append("\n");
        sb.append("    debugCrowdId: ").append(toIndentedString(debugCrowdId)).append("\n");
        sb.append("    crowdTargetType: ").append(toIndentedString(crowdTargetType)).append("\n");
        sb.append("    holdingBuckets: ").append(toIndentedString(holdingBuckets)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }

    public void init() {
        if (StringUtils.isEmpty(this.crowdTargetType)) {
            if (!StringUtils.isEmpty(this.filter)) {
                this.diversionBucket = new FilterDiversionBucket(this.filter);
            }

            if (this.crowdUsers != null && this.crowdUsers.size() > 0) {
                this.diversionBucket = new CrowdDiversionBucket(this.crowdUsers);
            }
        } else if (Constants.CrowdTargetType_CrowdId.equals(this.crowdTargetType)) {
            if (this.crowdUsers != null && this.crowdUsers.size() > 0) {
                this.diversionBucket = new CrowdDiversionBucket(this.crowdUsers);
            }
        } else if (Constants.CrowdTargetType_Filter.equals(this.crowdTargetType)) {
            if (!StringUtils.isEmpty(this.filter)) {
                this.diversionBucket = new FilterDiversionBucket(this.filter);
            }

        } else if (Constants.CrowdTargetType_Random.equals(this.crowdTargetType)) {
            this.diversionBucket = new UidDiversionBucket(100, this.holdingBuckets);
        }

        if (!StringUtils.isEmpty(this.debugUsers)) {
            String[] users = this.debugUsers.split(",");
            for (String user : users) {
                this.debugUserMap.put(user, true);
            }
        }
        if(this.debugCrowdUsers != null && this.debugCrowdUsers.size() > 0 ) {
            for (String user : this.debugCrowdUsers) {
                this.debugUserMap.put(user, true);
            }
        }

        if (!StringUtils.isEmpty(this.expGroupConfig)) {
            this.experimentParams = json.deserialize(this.expGroupConfig, this.experimentParams.getClass());
        }
    }

    public void addExperiment(Experiment experiment) {
        experimentList.add(experiment);
    }

    public boolean matchDebugUser(ExperimentContext experimentContext) {
        return this.debugUserMap.containsKey(experimentContext.getUid());
    }
    public boolean match(ExperimentContext experimentContext) {
        if (StringUtils.isEmpty(this.crowdTargetType)) {
            if (StringUtils.isEmpty(this.filter)
                    && (this.crowdUsers == null || this.crowdUsers.size() == 0)) {
                return true;
            }
            if (null != this.diversionBucket) {
                return this.diversionBucket.match(experimentContext);
            }
            return false;

        } else if (Constants.CrowdTargetType_ALL.equals(this.crowdTargetType)){
            return true;
        } else  {
            if (null != this.diversionBucket) {
                return this.diversionBucket.match(experimentContext);
            }
        }

        return false;
    }

    public List<Experiment> getExperimentList() {
        return experimentList;
    }

    public Map<String, Object> getExperimentParams() {
        return experimentParams;
    }
}
