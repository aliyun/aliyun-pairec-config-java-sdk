package com.aliyun.openservices.pairec.recallengine.flink.sink;

import com.aliyun.openservices.pairec.recallengine.RecallEngineClient;
import com.aliyun.openservices.pairec.recallengine.WriteRequest;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.table.data.ArrayData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.ArrayType;
import org.apache.flink.table.types.logical.BigIntType;
import org.apache.flink.table.types.logical.BooleanType;
import org.apache.flink.table.types.logical.DateType;
import org.apache.flink.table.types.logical.DoubleType;
import org.apache.flink.table.types.logical.FloatType;
import org.apache.flink.table.types.logical.IntType;
import org.apache.flink.table.types.logical.LocalZonedTimestampType;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.table.types.logical.TimestampType;
import org.apache.flink.table.types.logical.VarCharType;
import org.apache.flink.types.RowKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecallEngineSinkFunction implements SinkFunction<RowData> {
    
    private static final Logger LOG = LoggerFactory.getLogger(RecallEngineSinkFunction.class);
    
    private final String endpoint;
    private final String instanceId;
    private final String table;
    private final String username;
    private final String password;
    private final int retryTimes;
    private final String authorization;
    private final List<RowType.RowField> fields;
    
    private transient RecallEngineClient client;
    
    public RecallEngineSinkFunction(
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
        
        RowType rowType = (RowType) dataType.getLogicalType();
        this.fields = rowType.getFields();
    }
    
    private void initializeClient() {
        if (this.client == null) {
            this.client = new RecallEngineClient(endpoint, username, password)
                    .withRetryTimes(retryTimes);
            
            if (authorization != null && !authorization.isEmpty()) {
                this.client.withRequestHeader("Authorization", authorization);
            }
        }
    }
    
    @Override
    public void invoke(RowData value, Context context) throws Exception {
        initializeClient();
        
        LOG.debug("before write value:{}, row kind:{}", value, value.getRowKind());
        
        if (value.getRowKind() == RowKind.INSERT || value.getRowKind() == RowKind.UPDATE_AFTER) {
            Map<String, Object> data = new HashMap<>(fields.size());
            
            for (int i = 0; i < fields.size(); i++) {
                RowType.RowField rowField = fields.get(i);
                Object fieldValue = extractFieldValue(value, i, rowField.getType());
                if (fieldValue != null) {
                    data.put(rowField.getName(), fieldValue);
                }
            }
            
            LOG.debug("write data:{}", data);
            
            List<Map<String, Object>> content = new ArrayList<>();
            content.add(data);
            
            WriteRequest request = new WriteRequest();
            request.setContent(content);
            
            client.write(instanceId, table, request);
        }
    }
    
    private Object extractFieldValue(RowData value, int index, LogicalType type) {
        if (value.isNullAt(index)) {
            return null;
        }
        
        if (type instanceof IntType) {
            return value.getInt(index);
        } else if (type instanceof BigIntType) {
            return value.getLong(index);
        } else if (type instanceof FloatType) {
            return value.getFloat(index);
        } else if (type instanceof DoubleType) {
            return value.getDouble(index);
        } else if (type instanceof VarCharType) {
            return value.getString(index).toString();
        } else if (type instanceof BooleanType) {
            return value.getBoolean(index);
        } else if (type instanceof TimestampType) {
            // Convert TIMESTAMP to string
            return value.getTimestamp(index, ((TimestampType) type).getPrecision()).toString();
        } else if (type instanceof LocalZonedTimestampType) {
            // Convert LOCAL_ZONED_TIMESTAMP to string
            return value.getTimestamp(index, ((LocalZonedTimestampType) type).getPrecision()).toString();
        } else if (type instanceof DateType) {
            // Convert DATE to string (format: yyyy-MM-dd)
            int daysSinceEpoch = value.getInt(index);
            LocalDate date = LocalDate.ofEpochDay(daysSinceEpoch);
            return date.toString();
        } else if (type instanceof ArrayType) {
            return extractArrayValue(value.getArray(index), (ArrayType) type);
        } else {
            // Fallback for other types
            return value.getString(index).toString();
        }
    }
    
    private Object extractArrayValue(ArrayData arrayData, ArrayType arrayType) {
        if (arrayData == null || arrayData.size() == 0) {
            return null;
        }
        
        LogicalType elementType = arrayType.getElementType();
        
        if (elementType instanceof IntType) {
            int[] intArray = new int[arrayData.size()];
            for (int j = 0; j < arrayData.size(); j++) {
                intArray[j] = arrayData.getInt(j);
            }
            return intArray;
        } else if (elementType instanceof BigIntType) {
            long[] longArray = new long[arrayData.size()];
            for (int j = 0; j < arrayData.size(); j++) {
                longArray[j] = arrayData.getLong(j);
            }
            return longArray;
        } else if (elementType instanceof FloatType) {
            float[] floatArray = new float[arrayData.size()];
            for (int j = 0; j < arrayData.size(); j++) {
                floatArray[j] = arrayData.getFloat(j);
            }
            return floatArray;
        } else if (elementType instanceof DoubleType) {
            double[] doubleArray = new double[arrayData.size()];
            for (int j = 0; j < arrayData.size(); j++) {
                doubleArray[j] = arrayData.getDouble(j);
            }
            return doubleArray;
        } else if (elementType instanceof VarCharType) {
            String[] stringArray = new String[arrayData.size()];
            for (int j = 0; j < arrayData.size(); j++) {
                stringArray[j] = arrayData.getString(j).toString();
            }
            return stringArray;
        } else {
            return arrayData;
        }
    }
}
