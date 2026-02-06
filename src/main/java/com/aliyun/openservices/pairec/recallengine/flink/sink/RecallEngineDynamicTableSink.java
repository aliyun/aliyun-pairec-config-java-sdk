package com.aliyun.openservices.pairec.recallengine.flink.sink;

import com.aliyun.openservices.pairec.recallengine.flink.factory.RecallEngineTableFactory;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.connector.sink.SinkFunctionProvider;
import org.apache.flink.table.types.DataType;
import org.apache.flink.types.RowKind;

public class RecallEngineDynamicTableSink implements DynamicTableSink {
    
    private final String endpoint;
    private final String instanceId;
    private final String table;
    private final String username;
    private final String password;
    private final int retryTimes;
    private final String authorization;
    private final DataType dataType;
    
    public RecallEngineDynamicTableSink(
            String endpoint,
            String instanceId,
            String table,
            String username,
            String password,
            int retryTimes,
            String authorization,
            DataType dataType) {
        this.endpoint = endpoint;
        this.instanceId = instanceId;
        this.table = table;
        this.username = username;
        this.password = password;
        this.retryTimes = retryTimes;
        this.authorization = authorization;
        this.dataType = dataType;
    }
    
    @Override
    public ChangelogMode getChangelogMode(ChangelogMode requestedMode) {
        return ChangelogMode.newBuilder()
                .addContainedKind(RowKind.INSERT)
                .addContainedKind(RowKind.UPDATE_AFTER)
                .build();
    }
    
    @Override
    public SinkRuntimeProvider getSinkRuntimeProvider(Context context) {
        RecallEngineSinkFunction sinkFunction = new RecallEngineSinkFunction(
                endpoint, instanceId, table, username, password,
                retryTimes, authorization, dataType);
        return SinkFunctionProvider.of(sinkFunction);
    }
    
    @Override
    public DynamicTableSink copy() {
        return new RecallEngineDynamicTableSink(
                endpoint, instanceId, table, username, password,
                retryTimes, authorization, dataType);
    }
    
    @Override
    public String asSummaryString() {
        return RecallEngineTableFactory.IDENTIFIER;
    }
}
