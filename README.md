# aliyun-pairec-config-java-sdk

Java sdk for PA-REC config server. Aliyun product [link](https://pairec.console.aliyun.com/v2).

## Installation
```
<dependency>
  <groupId>com.aliyun.openservices.aiservice</groupId>
  <artifactId>pairec-sdk</artifactId>
  <version>1.0.9</version>
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
## Recall Engine request parameters (unreleased)

The following parameters are available on master and are not included in 1.0.9.
They match the Go SDK recall request model. These parameters require a Recall Engine
service version that supports them; upgrading the SDK alone does not add server-side support.

```java
import com.aliyun.openservices.pairec.recallengine.RecallConf;
import com.aliyun.openservices.pairec.recallengine.RecallEngineClient;
import com.aliyun.openservices.pairec.recallengine.RecallOptions;
import com.aliyun.openservices.pairec.recallengine.RecallRequest;
import com.aliyun.openservices.pairec.recallengine.RecallResponse;
import java.util.Collections;

RecallEngineClient client = new RecallEngineClient(
    System.getenv("RECALL_ENDPOINT"),
    System.getenv("RECALL_USERNAME"),
    System.getenv("RECALL_PASSWORD"));
// For a public endpoint, also supply its gateway token. Preserve the endpoint path prefix.
String token = System.getenv("RECALL_GATEWAY_TOKEN");
if (token != null && !token.isEmpty()) {
    client.withRequestHeader("Authorization", token);
}

RecallOptions options = new RecallOptions();
options.setTriggerLimit(10);
options.setTimeout(50); // Server-side per-way timeout in milliseconds, not HTTP timeout.
RecallConf recall = new RecallConf("trigger", 10);
recall.setOptions(options);

RecallRequest request = new RecallRequest();
request.setInstanceId(System.getenv("RECALL_INSTANCE_ID"));
request.setService(System.getenv("RECALL_SERVICE"));
request.setUid("123");
request.setRecalls(Collections.singletonMap("default", recall));
RecallResponse response = client.recall(request);
```

This serializes the recall way as:

```json
{"trigger":"trigger","count":10,"options":{"trigger_limit":10,"timeout":50}}
```

For trigger-based vector recall, select the item and user embedding table versions separately:

```java
RecallConf vectorRecall = new RecallConf("user_id", 10);
vectorRecall.setVersionId("item_version");
vectorRecall.setUserEmbeddingVersionId("user_version");
```

For explicit single- or multi-vector recall, use `queries` instead of a trigger:

```java
RecallConf vectorRecall = new RecallConf("", 10);
vectorRecall.setQueries(new float[][] {{1.0f, 0.0f}, {0.0f, 1.0f}});
vectorRecall.setVersionId("item_version");
```

Use the configured vector recall way name in `request.setRecalls(...)`; vector dimensions
must match the item vector index. Nonempty `queries` cannot be combined with a nonempty
`trigger`, `user_embedding_version_id`, or nonzero `trigger_limit`. The service validates
query counts, dimensions and supported operators.

`RecallRequest.version` selects a recall service version; `RecallConf.version_id` and
`user_embedding_version_id` select vector table versions. Empty version IDs and null/empty
`queries` are omitted, as in the Go SDK. An unset `options` is omitted; zero option values
are omitted, so `new RecallOptions()` serializes as `{}`. Existing constructors and
requests without these parameters retain their behavior. Responses continue to use
FlatBuffers, decoded by `RecallEngineClient` into `RecallResponse`.

## 版本说明
1.0.9 (2026-05-18)
* 公网调用链路优化
* recall engine delete api 支持

1.0.8 (2026-03-23)
* 支持InsertMode更新数据，upsert 部分更新字段功能支持KV类型的表
* write 写入接口支持异步写入

1.0.7 (2026-02-03)
* 召回管理client发布 

1.0.6 (2025-11-17)
* 全场景实验的支持 
 
1.0.5 (2024-07-05)
* 修复实验组设置过滤条件时，上下文不传过滤参数时抛出异常
 
1.0.4 (2024-04-17)
* 修复实验组随机流量匹配bug, 无法匹配所有人的实验组
 
1.0.3 (2024-03-23)
* 增加实验组随机流量的匹配逻辑

1.0.2 (2024-03-07)
* 优先匹配带有过滤条件或者指定人群的实验组
