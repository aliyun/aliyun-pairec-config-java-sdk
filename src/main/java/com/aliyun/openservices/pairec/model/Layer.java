package com.aliyun.openservices.pairec.model;

import com.google.gson.annotations.SerializedName;

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
}
