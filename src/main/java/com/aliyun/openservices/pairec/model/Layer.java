package com.aliyun.openservices.pairec.model;

import com.aliyun.openservices.pairec.common.Constants;
import com.aliyun.openservices.pairec.util.FNV;
import com.aliyun.tea.utils.StringUtils;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.codec.digest.DigestUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Layer
 */

public class Layer {
    @SerializedName("layer_id")
    private Long layerId = null;

    @SerializedName("exp_room_id")
    private Long expRoomId = null;

    @SerializedName("scene_id")
    private Long sceneId = null;

    @SerializedName("layer_name")
    private String layerName = null;

    @SerializedName("layer_info")
    private String layerInfo = null;

    List<ExperimentGroup> experimentGroupList = new ArrayList<>();

    public Layer layerId(Long layerId) {
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

    public Layer expRoomId(Long expRoomId) {
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

    public Layer sceneId(Long sceneId) {
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

    public Layer layerName(String layerName) {
        this.layerName = layerName;
        return this;
    }

    /**
     * Get layerName
     *
     * @return layerName
     **/
    public String getLayerName() {
        return layerName;
    }

    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }

    public Layer layerInfo(String layerInfo) {
        this.layerInfo = layerInfo;
        return this;
    }

    /**
     * Get layerInfo
     *
     * @return layerInfo
     **/
    public String getLayerInfo() {
        return layerInfo;
    }

    public void setLayerInfo(String layerInfo) {
        this.layerInfo = layerInfo;
    }


    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Layer layer = (Layer) o;
        return Objects.equals(this.layerId, layer.layerId) &&
                Objects.equals(this.expRoomId, layer.expRoomId) &&
                Objects.equals(this.sceneId, layer.sceneId) &&
                Objects.equals(this.layerName, layer.layerName) &&
                Objects.equals(this.layerInfo, layer.layerInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(layerId, expRoomId, sceneId, layerName, layerInfo);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Layer {\n");

        sb.append("    layerId: ").append(toIndentedString(layerId)).append("\n");
        sb.append("    expRoomId: ").append(toIndentedString(expRoomId)).append("\n");
        sb.append("    sceneId: ").append(toIndentedString(sceneId)).append("\n");
        sb.append("    layerName: ").append(toIndentedString(layerName)).append("\n");
        sb.append("    layerInfo: ").append(toIndentedString(layerInfo)).append("\n");
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

    public void addExperimentGroup(ExperimentGroup experimentGroup) {
        experimentGroupList.add(experimentGroup);
    }

    public List<ExperimentGroup> getExperimentGroupList() {
        return experimentGroupList;
    }

    public ExperimentGroup findMatchExperimentGroup( ExperimentContext experimentContext)  {

        // first find debug users
        for (ExperimentGroup experimentGroup : this.experimentGroupList) {
            if (experimentGroup.matchDebugUser(experimentContext)) {
                return experimentGroup;
            }
        }

        // find filter or crowdid experiment group
        for (ExperimentGroup experimentGroup : this.experimentGroupList) {
            if (StringUtils.isEmpty(experimentGroup.getCrowdTargetType())) {
                if (!StringUtils.isEmpty(experimentGroup.getFilter()) || (experimentGroup.getCrowdId() != null && experimentGroup.getCrowdId() > 0)) {
                    if (experimentGroup.match(experimentContext)) {
                        return  experimentGroup;
                    }
                }
            } else if (Constants.CrowdTargetType_Filter.equals(experimentGroup.getCrowdTargetType()) ||
                    Constants.CrowdTargetType_CrowdId.equals(experimentGroup.getCrowdTargetType())) {
                if (experimentGroup.match(experimentContext)) {
                    return  experimentGroup;
                }
            }
        }

        // find random experiment group
        for (ExperimentGroup experimentGroup : this.experimentGroupList) {
            if (Constants.CrowdTargetType_Random.equals(experimentGroup.getCrowdTargetType())) {
                StringBuilder hashKey = new StringBuilder(experimentContext.getUid());
                hashKey.append("_EXPROOM").append(this.getExpRoomId())
                        .append("_LAYER").append(this.getLayerId());

                String hashValue = hashValue(hashKey.toString());

                ExperimentContext randomExperimentContext = new ExperimentContext();
                randomExperimentContext.setUid(hashValue);
                randomExperimentContext.setFilterParams(experimentContext.getFilterParams());
                if (experimentGroup.match(randomExperimentContext)) {
                    return experimentGroup;
                }
            }
        }


        for (ExperimentGroup experimentGroup : this.experimentGroupList) {
            if (StringUtils.isEmpty(experimentGroup.getCrowdTargetType())) {
                if (StringUtils.isEmpty(experimentGroup.getFilter()) && (experimentGroup.getCrowdId() == null  ||  experimentGroup.getCrowdUsers().size() == 0)) {
                        return  experimentGroup;
                }
            } else if (Constants.CrowdTargetType_ALL.equals(experimentGroup.getCrowdTargetType())) {
                return experimentGroup;
            }
        }

        return null;
    }
    private String hashValue(String hashKey) {
        byte[] md5Hex = DigestUtils.md5(hashKey);
        BigInteger value = (new FNV()).fnv1_64(md5Hex);

        return value.toString();
    }
}
