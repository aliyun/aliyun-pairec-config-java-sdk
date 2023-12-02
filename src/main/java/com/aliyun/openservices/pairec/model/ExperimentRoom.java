package com.aliyun.openservices.pairec.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.aliyun.openservices.pairec.common.Constants;
import com.aliyun.tea.utils.StringUtils;
import com.google.gson.annotations.SerializedName;


/**
 * ExperimentRoom
 */

public class ExperimentRoom {
    @SerializedName("exp_room_id")
    private Long expRoomId = null;

    @SerializedName("scene_id")
    private Long sceneId = null;

    @SerializedName("exp_room_name")
    private String expRoomName = null;

    @SerializedName("exp_room_info")
    private String expRoomInfo = null;

    @SerializedName("debug_users")
    private String debugUsers = null;

    @SerializedName("bucket_count")
    private Integer bucketCount = null;

    @SerializedName("exp_room_buckets")
    private String expRoomBuckets = null;

    @SerializedName("bucket_type")
    private Integer bucketType = null;

    @SerializedName("filter")
    private String filter = null;

    @SerializedName("exp_room_config")
    private String expRoomConfig = null;

    @SerializedName("environment")
    private String environment = null;

    @SerializedName("type")
    private Integer type = null;

    @SerializedName("status")
    private Integer status = null;

    private Integer debugCrowdId = null;

    private List<String> debugCrowdIdUsers = null;
    private Map<String, Boolean> debugUserMap = new HashMap<>();

    DiversionBucket diversionBucket = null;

    private List<Layer> layerList = new ArrayList<>();

    public ExperimentRoom expRoomId(Long expRoomId) {
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

    public ExperimentRoom sceneId(Long sceneId) {
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

    public ExperimentRoom expRoomName(String expRoomName) {
        this.expRoomName = expRoomName;
        return this;
    }

    /**
     * Get expRoomName
     *
     * @return expRoomName
     **/
    public String getExpRoomName() {
        return expRoomName;
    }

    public void setExpRoomName(String expRoomName) {
        this.expRoomName = expRoomName;
    }

    public ExperimentRoom expRoomInfo(String expRoomInfo) {
        this.expRoomInfo = expRoomInfo;
        return this;
    }

    /**
     * Get expRoomInfo
     *
     * @return expRoomInfo
     **/
    public String getExpRoomInfo() {
        return expRoomInfo;
    }

    public void setExpRoomInfo(String expRoomInfo) {
        this.expRoomInfo = expRoomInfo;
    }

    public ExperimentRoom debugUsers(String debugUsers) {
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

    public ExperimentRoom bucketCount(Integer bucketCount) {
        this.bucketCount = bucketCount;
        return this;
    }

    /**
     * Get bucketCount
     *
     * @return bucketCount
     **/
    public Integer getBucketCount() {
        return bucketCount;
    }

    public void setBucketCount(Integer bucketCount) {
        this.bucketCount = bucketCount;
    }

    public ExperimentRoom expRoomBuckets(String expRoomBuckets) {
        this.expRoomBuckets = expRoomBuckets;
        return this;
    }

    /**
     * Get expRoomBuckets
     *
     * @return expRoomBuckets
     **/
    public String getExpRoomBuckets() {
        return expRoomBuckets;
    }

    public void setExpRoomBuckets(String expRoomBuckets) {
        this.expRoomBuckets = expRoomBuckets;
    }

    public ExperimentRoom bucketType(Integer bucketType) {
        this.bucketType = bucketType;
        return this;
    }

    /**
     * Get bucketType
     *
     * @return bucketType
     **/
    public Integer getBucketType() {
        return bucketType;
    }

    public void setBucketType(Integer bucketType) {
        this.bucketType = bucketType;
    }

    public ExperimentRoom filter(String filter) {
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

    public ExperimentRoom expRoomConfig(String expRoomConfig) {
        this.expRoomConfig = expRoomConfig;
        return this;
    }

    /**
     * Get expRoomConfig
     *
     * @return expRoomConfig
     **/
    public String getExpRoomConfig() {
        return expRoomConfig;
    }

    public void setExpRoomConfig(String expRoomConfig) {
        this.expRoomConfig = expRoomConfig;
    }

    public ExperimentRoom environment(String environment) {
        this.environment = environment;
        return this;
    }

    /**
     * Get environment
     *
     * @return environment
     **/
    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public ExperimentRoom type(Integer type) {
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

    public ExperimentRoom status(Integer status) {
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

    public Integer getDebugCrowdId() {
        return debugCrowdId;
    }

    public void setDebugCrowdId(Integer debugCrowdId) {
        this.debugCrowdId = debugCrowdId;
    }

    public List<String> getDebugCrowdIdUsers() {
        return debugCrowdIdUsers;
    }

    public void setDebugCrowdIdUsers(List<String> debugCrowdIdUsers) {
        this.debugCrowdIdUsers = debugCrowdIdUsers;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExperimentRoom experimentRoom = (ExperimentRoom) o;
        return Objects.equals(this.expRoomId, experimentRoom.expRoomId) &&
                Objects.equals(this.sceneId, experimentRoom.sceneId) &&
                Objects.equals(this.expRoomName, experimentRoom.expRoomName) &&
                Objects.equals(this.expRoomInfo, experimentRoom.expRoomInfo) &&
                Objects.equals(this.debugUsers, experimentRoom.debugUsers) &&
                Objects.equals(this.bucketCount, experimentRoom.bucketCount) &&
                Objects.equals(this.expRoomBuckets, experimentRoom.expRoomBuckets) &&
                Objects.equals(this.bucketType, experimentRoom.bucketType) &&
                Objects.equals(this.filter, experimentRoom.filter) &&
                Objects.equals(this.expRoomConfig, experimentRoom.expRoomConfig) &&
                Objects.equals(this.environment, experimentRoom.environment) &&
                Objects.equals(this.type, experimentRoom.type) &&
                Objects.equals(this.debugCrowdId, experimentRoom.debugCrowdId) &&
                Objects.equals(this.debugCrowdIdUsers, experimentRoom.debugCrowdIdUsers) &&
                Objects.equals(this.status, experimentRoom.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expRoomId, sceneId, expRoomName, expRoomInfo, debugUsers, bucketCount, expRoomBuckets, bucketType, filter, expRoomConfig, environment,
                type, status, debugCrowdId, debugCrowdIdUsers);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ExperimentRoom {\n");

        sb.append("    expRoomId: ").append(toIndentedString(expRoomId)).append("\n");
        sb.append("    sceneId: ").append(toIndentedString(sceneId)).append("\n");
        sb.append("    expRoomName: ").append(toIndentedString(expRoomName)).append("\n");
        sb.append("    expRoomInfo: ").append(toIndentedString(expRoomInfo)).append("\n");
        sb.append("    debugUsers: ").append(toIndentedString(debugUsers)).append("\n");
        sb.append("    bucketCount: ").append(toIndentedString(bucketCount)).append("\n");
        sb.append("    expRoomBuckets: ").append(toIndentedString(expRoomBuckets)).append("\n");
        sb.append("    bucketType: ").append(toIndentedString(bucketType)).append("\n");
        sb.append("    filter: ").append(toIndentedString(filter)).append("\n");
        sb.append("    expRoomConfig: ").append(toIndentedString(expRoomConfig)).append("\n");
        sb.append("    environment: ").append(toIndentedString(environment)).append("\n");
        sb.append("    type: ").append(toIndentedString(type)).append("\n");
        sb.append("    status: ").append(toIndentedString(status)).append("\n");
        sb.append("    debugCrowdId: ").append(toIndentedString(debugCrowdId)).append("\n");
        sb.append("    debugCrowdIdUsers: ").append(toIndentedString(debugCrowdIdUsers)).append("\n");
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

    /**
     * Experiment Room init function
     */
    public void init() {
        if (!StringUtils.isEmpty(this.debugUsers)) {
            String[] users = this.debugUsers.split(",");
            for (String user : users) {
                this.debugUserMap.put(user, true);
            }
        }
        if (this.debugCrowdIdUsers != null && this.debugCrowdIdUsers.size() > 0) {
            for(String user : this.debugCrowdIdUsers) {
                this.debugUserMap.put(user, true);
            }
        }

        if (diversionBucket == null) {
            if (bucketType == Constants.Bucket_Type_UID) {
                diversionBucket = new UidDiversionBucket(this.bucketCount, this.expRoomBuckets);
            } else if (bucketType == Constants.Bucket_Type_UID_HASH) {
                diversionBucket = new UidHashDiversionBucket(this.bucketCount, this.expRoomBuckets);
            } else if (bucketType == Constants.Bucket_Type_Custom) {
                diversionBucket = new CustomDiversionBucket();
            } else if (bucketType == Constants.Bucket_Type_Filter) {
                diversionBucket = new FilterDiversionBucket(this.filter);
            }
        }
    }

    public void addLayer(Layer layer) {
        this.layerList.add(layer);
    }

    public boolean matchDebugUsers(ExperimentContext experimentContext) {
        return this.debugUserMap.containsKey(experimentContext.getUid());
    }
    public boolean match(ExperimentContext experimentContext) {
        if (this.debugUserMap.containsKey(experimentContext.getUid())) {
            return true;
        }

        if (null != this.diversionBucket) {
            return this.diversionBucket.match(experimentContext);
        }
        return false;
    }

    public List<Layer> getLayerList() {
        return layerList;
    }
}
