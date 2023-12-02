package com.aliyun.openservices.pairec.model;

import java.util.Objects;

import com.google.gson.annotations.SerializedName;

/**
 * Param
 */

public class Param {
  @SerializedName("param_id")
  private Long paramId = null;

  @SerializedName("scene_id")
  private Long sceneId = null;

  @SerializedName("param_name")
  private String paramName = null;

  @SerializedName("param_value")
  private String paramValue = null;

  @SerializedName("environment")
  private String environment = null;

  public Param paramId(Long paramId) {
    this.paramId = paramId;
    return this;
  }

   /**
   * Get paramId
   * @return paramId
  **/
  public Long getParamId() {
    return paramId;
  }

  public void setParamId(Long paramId) {
    this.paramId = paramId;
  }

  public Param sceneId(Long sceneId) {
    this.sceneId = sceneId;
    return this;
  }

   /**
   * Get sceneId
   * @return sceneId
  **/
  public Long getSceneId() {
    return sceneId;
  }

  public void setSceneId(Long sceneId) {
    this.sceneId = sceneId;
  }

  public Param paramName(String paramName) {
    this.paramName = paramName;
    return this;
  }

   /**
   * Get paramName
   * @return paramName
  **/
  public String getParamName() {
    return paramName;
  }

  public void setParamName(String paramName) {
    this.paramName = paramName;
  }

  public Param paramValue(String paramValue) {
    this.paramValue = paramValue;
    return this;
  }

   /**
   * Get paramValue
   * @return paramValue
  **/
  public String getParamValue() {
    return paramValue;
  }

  public void setParamValue(String paramValue) {
    this.paramValue = paramValue;
  }

  public Param environment(String environment) {
    this.environment = environment;
    return this;
  }

   /**
   * Get environment
   * @return environment
  **/
  public String getEnvironment() {
    return environment;
  }

  public void setEnvironment(String environment) {
    this.environment = environment;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Param param = (Param) o;
    return Objects.equals(this.paramId, param.paramId) &&
        Objects.equals(this.sceneId, param.sceneId) &&
        Objects.equals(this.paramName, param.paramName) &&
        Objects.equals(this.paramValue, param.paramValue) &&
        Objects.equals(this.environment, param.environment);
  }

  @Override
  public int hashCode() {
    return Objects.hash(paramId, sceneId, paramName, paramValue, environment);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Param {\n");
    
    sb.append("    paramId: ").append(toIndentedString(paramId)).append("\n");
    sb.append("    sceneId: ").append(toIndentedString(sceneId)).append("\n");
    sb.append("    paramName: ").append(toIndentedString(paramName)).append("\n");
    sb.append("    paramValue: ").append(toIndentedString(paramValue)).append("\n");
    sb.append("    environment: ").append(toIndentedString(environment)).append("\n");
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

}
