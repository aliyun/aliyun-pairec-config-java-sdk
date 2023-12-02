package com.aliyun.openservices.pairec.api;


import com.aliyun.openservices.pairec.common.Constants;
import com.aliyun.teaopenapi.models.Config;

public class Configuration {
    private String instanceId;

    private String domain = null;

    private Config config;

    private  String environment = Constants.Environment_Daily_Desc;

    public boolean isVpc() {
        return vpc;
    }

    public void setVpc(boolean vpc) {
        this.vpc = vpc;
    }

    private boolean vpc = false;

    public Configuration(String regionId, String accessKeyId, String accessKeySecret, String instanceId) {
        this.config = new Config();
        this.config.setAccessKeyId(accessKeyId);
        this.config.setAccessKeySecret(accessKeySecret);
        this.config.setType("access_key");
        this.config.setRegionId(regionId);
        this.instanceId = instanceId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getDomain() {
        if (this.domain == null) {
            if (this.vpc) {
                this.domain = "pairecservice-vpc." +this.config.getRegionId() + ".aliyuncs.com";
            } else {
                this.domain = "pairecservice." +this.config.getRegionId() + ".aliyuncs.com";
            }
        }
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Config getConfig() {
        this.config.setEndpoint(this.getDomain());
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public String getEnvironment() {
        return environment;
    }

    public  void setEnvironment(String environment) {
        this.environment = environment;
    }
}
