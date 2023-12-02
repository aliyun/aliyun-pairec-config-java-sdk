package com.aliyun.openservices.pairec.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;

/**
 * Scene
 */

public class Scene {
    @SerializedName("scene_id")
    private Long sceneId = null;

    @SerializedName("scene_name")
    private String sceneName = null;

    @SerializedName("scene_info")
    private String sceneInfo = null;

    private List<ExperimentRoom> experimentRooms = new ArrayList<>();

    public Scene sceneId(Long sceneId) {
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

    public Scene sceneName(String sceneName) {
        this.sceneName = sceneName;
        return this;
    }

    /**
     * Get sceneName
     *
     * @return sceneName
     **/
    public String getSceneName() {
        return sceneName;
    }

    public void setSceneName(String sceneName) {
        this.sceneName = sceneName;
    }

    public Scene sceneInfo(String sceneInfo) {
        this.sceneInfo = sceneInfo;
        return this;
    }

    /**
     * Get sceneInfo
     *
     * @return sceneInfo
     **/
    public String getSceneInfo() {
        return sceneInfo;
    }

    public void setSceneInfo(String sceneInfo) {
        this.sceneInfo = sceneInfo;
    }


    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Scene scene = (Scene) o;
        return Objects.equals(this.sceneId, scene.sceneId) &&
                Objects.equals(this.sceneName, scene.sceneName) &&
                Objects.equals(this.sceneInfo, scene.sceneInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sceneId, sceneName, sceneInfo);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Scene {\n");

        sb.append("    sceneId: ").append(toIndentedString(sceneId)).append("\n");
        sb.append("    sceneName: ").append(toIndentedString(sceneName)).append("\n");
        sb.append("    sceneInfo: ").append(toIndentedString(sceneInfo)).append("\n");
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

    public void addExperimentRoom(ExperimentRoom experimentRoom) {
        this.experimentRooms.add(experimentRoom);
    }

    public List<ExperimentRoom> getExperimentRooms() {
        return experimentRooms;
    }
}
