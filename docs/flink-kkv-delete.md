# 用 Flink SQL 删除 Recall Engine KKV 数据

适用于 v2 KKV（X2I）表。假设每条关系由 `tid`（pkey）和 `item_id`（skey）唯一确定，`u1` 下有 A、B 两个成员。上游发出一条 `(u1, A)` 的删除变更时，Sink 配置决定删除范围。

| 希望得到的结果 | Sink 的删除配置 | Recall Engine HTTP 接口 |
| --- | --- | --- |
| 删掉 A，保留 B | `delete_key_field = tid` 和 `delete_skey_field = item_id` | `delete_key_pairs` |
| 清空 u1，A、B 都删除 | 只配置 `delete_key_field = tid` | `delete_records` |

两种删除都对目标版本里的在线数据和 MC 导入数据生效。删除后重新写入一个成员，只恢复该成员。MC 源分区里的原始行仍由源表管理。

## 准备作业

1. 准备已存在的 v2 KKV 目标表，确认其主键、成员键和分数字段与下例一致。使用键对模式时，服务端需已启用 `delete_key_pairs`，在线和回放消费者均已支持该操作。
2. 在 Flink 作业中注册包含 `delete_skey_field` 的 Connector JAR。本文使用本分支构建的 JAR，在 Flink 1.20.3 上验证。如果自行构建，运行 `mvn -Pflink-1.20 -Dmaven.test.skip=true clean package`，注册 `target/pairec-sdk-1.0.9.jar`。同名同版本的其他构建可能只支持整组删除。
3. 准备能输出原生删除变更（`RowKind.DELETE`，简写 `-D`）的源。已有 CDC 源表或视图时，可直接使用下一节的 Sink SQL。

例如，使用 MySQL CDC 时，可按下面的 SQL 声明源表。MySQL 中的 `recommendation.x2i_edges` 应启用 binlog 并以 `(tid, item_id)` 为主键，作业环境需要 MySQL CDC Connector。将示例中的地址和凭据占位符替换为作业配置。

```sql
CREATE TEMPORARY TABLE source_edges (
    tid STRING NOT NULL,
    item_id STRING NOT NULL,
    score FLOAT,
    PRIMARY KEY (tid, item_id) NOT ENFORCED
) WITH (
    'connector' = 'mysql-cdc',
    'hostname' = '<mysql-host>',
    'port' = '3306',
    'username' = '<mysql-user>',
    'password' = '<mysql-password>',
    'database-name' = 'recommendation',
    'table-name' = 'x2i_edges'
);
```

## 按 pkey 和 skey 删除一个成员

```sql
CREATE TEMPORARY TABLE recall_sink (
    tid STRING NOT NULL,
    item_id STRING NOT NULL,
    score FLOAT,
    PRIMARY KEY (tid, item_id) NOT ENFORCED
) WITH (
    'connector' = 'recallengine',
    'endpoint' = '<recall-engine-http-endpoint>',
    'instance_id' = '<instance-id>',
    'table' = 'x2i_edges',
    'username' = '<recall-user>',
    'password' = '<recall-password>',
    'delete_key_field' = 'tid',
    'delete_skey_field' = 'item_id'
);

INSERT INTO recall_sink
SELECT tid, item_id, score FROM source_edges;
```

启动同步后，在 **MySQL 源表** 执行下面的语句。Flink 收到 `-D` 后，Sink 发送 `{"key_pairs":[{"pkey":"u1","skey":"A"}]}`。等消费者应用后，查询 `u1` 只剩 B，其他主键下的 A 仍保留。

```sql
DELETE FROM recommendation.x2i_edges
WHERE tid = 'u1' AND item_id = 'A';
```

## 按 pkey 删除整组

如果业务约定一条删除事件表示清空整个 `tid`，从上述 Sink DDL 的 `WITH` 中移除 `delete_skey_field`，保留以下删除配置，然后运行相同的 `INSERT INTO ... SELECT ...`。

```sql
'delete_key_field' = 'tid'
```

此时上游发出同一条 `(u1, A)` 删除变更，Sink 发送 `{"keys":["u1"]}`。查询 `u1` 会得到空组；随后写入 `(u1, D)`，组里只会出现 D。若 MySQL 中删除一行通常只表示删除一条关系，应使用上一节的双列配置。

## 配置与运行语义

- 两个配置项填写 **Sink 的物理列名**，支持 `STRING`、`VARCHAR`、`INT`、`BIGINT`。双列模式要求两个不同的列，删除行须提供两个有效键。缺失或空次键会使作业失败。
- `PRIMARY KEY` 描述 Flink 表中一行的标识。删除范围由上述两个配置决定。普通追加流中的 `op='DELETE'` 是业务字段，须由上游转换成真正的 `-D`。
- `INSERT` 和 `UPDATE_AFTER` 走写入接口，KKV 保留默认的 `insert_mode='insert'`。Flink 无需指定 `version_id` 或 `write_mode`，回放消费者会把删除应用到正在构建的新版本。
- Sink 在同一子任务中先刷新已缓存的写入，再发送删除。服务端异步投递，HTTP 成功表示已受理。核对删除效果时，应等待消费完成后点查。同一键的写、删、重插需要保持顺序，整组模式要按 pkey 考虑上游分区。
