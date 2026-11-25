package com.aliyun.openservices.pairec.recallengine.flink.factory;

import com.aliyun.openservices.pairec.recallengine.InsertMode;
import com.aliyun.openservices.pairec.recallengine.flink.sink.RecallEngineDynamicTableSink;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.table.api.ValidationException;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.factories.DynamicTableSinkFactory;
import org.apache.flink.table.factories.FactoryUtil;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.RowType;

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

    public static final ConfigOption<String> INSERT_MODE = ConfigOptions.key("insert_mode")
            .stringType()
            .defaultValue("insert")
            .withDescription("Write mode: 'insert' (default) or 'upsert'");

    public static final ConfigOption<String> DELETE_KEY_FIELD = ConfigOptions.key("delete_key_field")
            .stringType()
            .noDefaultValue()
            .withDescription("Physical column containing the backend primary key; enables DELETE handling");

    public static final ConfigOption<String> DELETE_SKEY_FIELD = ConfigOptions.key("delete_skey_field")
            .stringType().noDefaultValue()
            .withDescription("Physical column containing the secondary key for KKV member deletion");

    public static final ConfigOption<Integer> BATCH_SIZE = ConfigOptions.key("batch_size")
            .intType()
            .defaultValue(200)
            .withDescription("Rows per write request; the sink flushes as soon as this many rows are buffered");

    public static final ConfigOption<Long> FLUSH_INTERVAL_MS = ConfigOptions.key("flush_interval_ms")
            .longType()
            .defaultValue(50L)
            .withDescription("Flush a partial batch after this many milliseconds");

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

        InsertMode insertMode = InsertMode.fromValue(options.get(INSERT_MODE));
        // Validate here rather than letting the client reject these when the
        // first record arrives: a bad value should fail job submission, not a
        // running job.
        final int batchSize = options.get(BATCH_SIZE);
        if (batchSize <= 0) {
            throw new ValidationException(
                    String.format("'%s' must be positive, got %d", BATCH_SIZE.key(), batchSize));
        }
        final long flushIntervalMs = options.get(FLUSH_INTERVAL_MS);
        if (flushIntervalMs <= 0) {
            throw new ValidationException(
                    String.format("'%s' must be positive, got %d", FLUSH_INTERVAL_MS.key(), flushIntervalMs));
        }

        final DataType producedDataType =
                context.getCatalogTable().getResolvedSchema().toPhysicalRowDataType();
        RowType rowType = (RowType) producedDataType.getLogicalType();
        int deleteKeyIndex = keyIndex(rowType, options, DELETE_KEY_FIELD);
        int deleteSkeyIndex = keyIndex(rowType, options, DELETE_SKEY_FIELD);
        if (deleteSkeyIndex >= 0 && (deleteKeyIndex < 0 || deleteKeyIndex == deleteSkeyIndex)) {
            throw new ValidationException("'delete_skey_field' requires a distinct 'delete_key_field'");
        }
        return new RecallEngineDynamicTableSink(
                endpoint, instanceId, table, username, password,
                retryTimes, authorization, insertMode, batchSize, flushIntervalMs,
                producedDataType, deleteKeyIndex, deleteSkeyIndex);
    }
    
    private static int keyIndex(RowType rowType, ReadableConfig options, ConfigOption<String> option) {
        String name = options.getOptional(option).orElse(null);
        if (name == null) return -1;
        int index = rowType.getFieldNames().indexOf(name);
        if (index < 0) throw new ValidationException(option.key() + " must reference a physical column: " + name);
        switch (rowType.getTypeAt(index).getTypeRoot()) {
            case VARCHAR:
            case INTEGER:
            case BIGINT:
                return index;
            default:
                throw new ValidationException(option.key() + " must be STRING, VARCHAR, INT or BIGINT: " + name);
        }
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
        options.add(INSERT_MODE);
        options.add(DELETE_KEY_FIELD);
        options.add(DELETE_SKEY_FIELD);
        options.add(BATCH_SIZE);
        options.add(FLUSH_INTERVAL_MS);
        return options;
    }
}
