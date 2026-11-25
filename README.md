# aliyun-pairec-config-java-sdk

Java sdk for PA-REC config server. Aliyun product [link](https://pairec.console.aliyun.com/v2).

## Installation
```
<dependency>
  <groupId>com.aliyun.openservices.aiservice</groupId>
  <artifactId>pairec-sdk</artifactId>
  <version>1.0.7</version>
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
## Flink DELETE support

For v2 KKV tables, configure both `delete_key_field` and `delete_skey_field`
to delete one member. Configure only `delete_key_field` to delete the entire group.

Set `delete_key_field` to the physical column containing the RecallEngine primary key
to enable native Flink `DELETE` handling. Supported key types are `STRING` / `VARCHAR`,
`INT`, and `BIGINT`. Each configured key column must exist and have a non-null
value on the `DELETE` row. String keys must also be non-empty.

```sql
CREATE TEMPORARY TABLE recall_sink (
    item_id STRING NOT NULL,
    score FLOAT
) WITH (
    'connector' = 'recallengine',
    'endpoint' = 'http://<endpoint>',
    'instance_id' = '<instance-id>',
    'table' = '<table-name>',
    'username' = '<username>',
    'password' = '<password>',
    'delete_key_field' = 'item_id'
);

-- The upstream table supplies native Flink changelog events.
INSERT INTO recall_sink
SELECT item_id, score FROM upstream_changelog;
```

`INSERT` and `UPDATE_AFTER` keep the existing write behavior. When `delete_key_field`
is configured, `DELETE` flushes earlier buffered writes and synchronously calls the
Recall Engine delete API; delete failures are propagated to Flink. `UPDATE_BEFORE` is not a
delete command. Without this option, the supported changelog remains unchanged.
An ordinary business column named `op` is not interpreted by the connector.

Deletion uses the existing target's `default` schema and default data version.
For KV / Vector tables, it removes the record for the primary key. With only
`delete_key_field`, a KKV / Bloom delete removes the **entire primary-key group**.
For member-level CDC deletes on v2 KKV, set `delete_skey_field` to a distinct
physical column holding the secondary key. Both keys must be present on the delete row.

Writes and deletes for the same backend key must arrive in order at the same sink
subtask. A composite SQL primary key does not ensure routing by a KKV / Bloom group
key. This option does not require a SQL primary-key declaration; Flink's planning
and `NOT NULL` constraints still apply, including to DELETE rows.

The sink flushes earlier buffered writes before sending a delete from the same
subtask. Both delete modes also flush at checkpoints and propagate a failed
write batch to Flink. HTTP acceptance precedes asynchronous consumption.

## 打包

```shell
mvn clean  -Dmaven.test.skip=true  package -P flink-1.20
```

## 版本说明
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
