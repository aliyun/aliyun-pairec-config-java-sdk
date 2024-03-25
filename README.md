# aliyun-pairec-config-java-sdk

Java sdk for PA-REC config server. Aliyun product [link](https://pairec.console.aliyun.com/v2).

## Installation
```
<dependency>
  <groupId>com.aliyun.openservices.aiservice</groupId>
  <artifactId>pairec-sdk</artifactId>
  <version>1.0.3</version>
</dependency>
```

## Usage

```java
package com.aliyun.openservices.pairec;

import com.aliyun.openservices.pairec.api.ApiClient;
import com.aliyun.openservices.pairec.api.Configuration;
import com.aliyun.openservices.pairec.common.Constants;
import com.aliyun.openservices.pairec.model.ExperimentContext;
import com.aliyun.openservices.pairec.model.ExperimentResult;

public class ExperimentTest {
    static ExperimentClient experimentClient;
    public static void main(String[] args) throws Exception {
        String regionId = "cn-hangzhou";
        String instanceId = System.getenv("INSTANCE_ID"); // pai-rec instance id
        String accessId = System.getenv("ACCESS_ID"); // aliyun accessKeyId
        String accessKey = System.getenv("ACCESS_KEY"); // aliyun accessKeySecret
        Configuration configuration = new Configuration(regionId, accessId, accessKey, instanceId);
        // set experiment environment
        configuration.setEnvironment(Constants.Environment_Product_Desc);
        ApiClient apiClient = new ApiClient(configuration);

        experimentClient = new ExperimentClient(apiClient);
        // init client
        experimentClient.init();
        // set expeirment context
        ExperimentContext experimentContext = new ExperimentContext();
        experimentContext.setUid("103441638");
        experimentContext.setRequestId("pvid");

        // match experiment, use scence and experimentContext
        ExperimentResult experimentResult = experimentClient.matchExperiment("home_feed", experimentContext);

        // exp id
        System.out.println(experimentResult.getExpId());
        // exp log info
        System.out.println(experimentResult.info());

        // get experiment param value
        System.out.println(experimentResult.getExperimentParams().getString("rank_version", "not exist"));
        System.out.println(experimentResult.getExperimentParams().getString("version", "not exist"));
        System.out.println(experimentResult.getExperimentParams().getString("recall", "not exist"));
        System.out.println(experimentResult.getExperimentParams().getDouble("recall_d", 0.0));

        // get experiment param value by pecific layer name
        System.out.println(experimentResult.getLayerParams("recall").getString("rank_version", "not exist"));
        System.out.println(experimentResult.getLayerParams("rank").getString("version", "not exist"));


    }
}

```
## 版本说明
1.0.3 (2024-03-23)
* 增加实验组随机流量的匹配逻辑

1.0.2 (2024-03-07)
* 优先匹配带有过滤条件或者指定人群的实验组
