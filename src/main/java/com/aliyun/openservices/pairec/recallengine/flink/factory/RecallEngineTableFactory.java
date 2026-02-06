package com.aliyun.openservices.pairec.recallengine.flink.factory;

import com.aliyun.openservices.pairec.recallengine.flink.sink.RecallEngineDynamicTableSink;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.factories.DynamicTableSinkFactory;
import org.apache.flink.table.factories.FactoryUtil;
import org.apache.flink.table.types.DataType;

import java.util.HashSet;
import java.util.Set;

public class RecallEngineTableFactory implements DynamicTableSinkFactory {
    
    public static final String IDENTIFIER = "recallengine";
    
    public static final ConfigOption<String> ENDPOINT = ConfigOptions.key("endpoint")
            .stringType()
            .noDefaultValue()
            .withDescription("RecallEngine service endpoint");
    
    public static final ConfigOption<String> INSTANCE_ID = ConfigOptions.key("instance_id")
            .stringType()
            .noDefaultValue()
            .withDescription("RecallEngine instance ID");
    
    public static final ConfigOption<String> TABLE = ConfigOptions.key("table")
            .stringType()
            .noDefaultValue()
            .withDescription("Target table name");
    
    public static final ConfigOption<String> USERNAME = ConfigOptions.key("username")
            .stringType()
            .noDefaultValue()
            .withDescription("Authentication username");
    
    public static final ConfigOption<String> PASSWORD = ConfigOptions.key("password")
            .stringType()
            .noDefaultValue()
            .withDescription("Authentication password");
    
    public static final ConfigOption<Integer> RETRY_TIMES = ConfigOptions.key("retry_times")
            .intType()
            .defaultValue(3)
            .withDescription("Number of retry attempts");
    
    public static final ConfigOption<String> AUTHORIZATION = ConfigOptions.key("authorization")
            .stringType()
            .noDefaultValue()
            .withDescription("Optional Authorization header value");
    
    @Override
    public DynamicTableSink createDynamicTableSink(Context context) {
        final FactoryUtil.TableFactoryHelper helper = FactoryUtil.createTableFactoryHelper(this, context);
        helper.validate();
        
        final ReadableConfig options = helper.getOptions();
        final String endpoint = options.get(ENDPOINT);
        final String instanceId = options.get(INSTANCE_ID);
        final String table = options.get(TABLE);
        final String username = options.get(USERNAME);
        final String password = options.get(PASSWORD);
        final int retryTimes = options.get(RETRY_TIMES);
        
        String authorization = null;
        if (options.getOptional(AUTHORIZATION).isPresent()) {
            authorization = options.get(AUTHORIZATION);
        }
        
        final DataType producedDataType =
                context.getCatalogTable().getResolvedSchema().toPhysicalRowDataType();
        
        return new RecallEngineDynamicTableSink(
                endpoint, instanceId, table, username, password, 
                retryTimes, authorization, producedDataType);
    }
    
    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }
    
    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        final Set<ConfigOption<?>> options = new HashSet<>();
        options.add(ENDPOINT);
        options.add(INSTANCE_ID);
        options.add(TABLE);
        options.add(USERNAME);
        options.add(PASSWORD);
        return options;
    }
    
    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        final Set<ConfigOption<?>> options = new HashSet<>();
        options.add(RETRY_TIMES);
        options.add(AUTHORIZATION);
        return options;
    }
}
