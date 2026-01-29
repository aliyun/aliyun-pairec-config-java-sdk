package com.aliyun.openservices.pairec.recallengine;

import com.aliyun.openservices.pairec.recallengine.recallenginefb.*;
import com.google.flatbuffers.Table;
import java.nio.ByteBuffer;

/**
 * Utility class for deserializing FlatBuffers data into Record objects
 */
public class RecordUtils {
    
    /**
     * Deserialize FlatBuffers byte array into a Record
     * 
     * @param data the FlatBuffers serialized data
     * @return the deserialized Record
     */
    public static Record unserializeRecord(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        MatchRecords matchRecords = MatchRecords.getRootAsMatchRecords(bb);
        
        Record record = new Record((int) matchRecords.docCount());
        int size = record.size();
        
        for (int i = 0; i < matchRecords.recordColumnsLength(); i++) {
            String name = matchRecords.fieldName(i);
            FieldValueColumnTable columnTable = matchRecords.recordColumns(i);
            
            if (columnTable == null) {
                continue;
            }
            
            byte columnType = columnTable.fieldValueColumnType();
            
            switch (columnType) {
                case FieldValueColumn.StringValueColumn:
                    StringValueColumn stringCol = new StringValueColumn();
                    columnTable.fieldValueColumn(stringCol);
                    deserializeStringColumn(record, name, size, stringCol);
                    break;
                    
                case FieldValueColumn.Int32ValueColumn:
                    Int32ValueColumn int32Col = new Int32ValueColumn();
                    columnTable.fieldValueColumn(int32Col);
                    deserializeInt32Column(record, name, size, int32Col);
                    break;
                    
                case FieldValueColumn.Int64ValueColumn:
                    Int64ValueColumn int64Col = new Int64ValueColumn();
                    columnTable.fieldValueColumn(int64Col);
                    deserializeInt64Column(record, name, size, int64Col);
                    break;
                    
                case FieldValueColumn.FloatValueColumn:
                    FloatValueColumn floatCol = new FloatValueColumn();
                    columnTable.fieldValueColumn(floatCol);
                    deserializeFloatColumn(record, name, size, floatCol);
                    break;
                    
                case FieldValueColumn.DoubleValueColumn:
                    DoubleValueColumn doubleCol = new DoubleValueColumn();
                    columnTable.fieldValueColumn(doubleCol);
                    deserializeDoubleColumn(record, name, size, doubleCol);
                    break;
                    
                case FieldValueColumn.BoolValueColumn:
                    BoolValueColumn boolCol = new BoolValueColumn();
                    columnTable.fieldValueColumn(boolCol);
                    deserializeBoolColumn(record, name, size, boolCol);
                    break;
                    
                case FieldValueColumn.MultiStringValueColumn:
                    MultiStringValueColumn multiStringCol = new MultiStringValueColumn();
                    columnTable.fieldValueColumn(multiStringCol);
                    deserializeMultiStringColumn(record, name, size, multiStringCol);
                    break;
                    
                case FieldValueColumn.MultiInt32ValueColumn:
                    MultiInt32ValueColumn multiInt32Col = new MultiInt32ValueColumn();
                    columnTable.fieldValueColumn(multiInt32Col);
                    deserializeMultiInt32Column(record, name, size, multiInt32Col);
                    break;
                    
                case FieldValueColumn.MultiInt64ValueColumn:
                    MultiInt64ValueColumn multiInt64Col = new MultiInt64ValueColumn();
                    columnTable.fieldValueColumn(multiInt64Col);
                    deserializeMultiInt64Column(record, name, size, multiInt64Col);
                    break;
                    
                case FieldValueColumn.MultiFloatValueColumn:
                    MultiFloatValueColumn multiFloatCol = new MultiFloatValueColumn();
                    columnTable.fieldValueColumn(multiFloatCol);
                    deserializeMultiFloatColumn(record, name, size, multiFloatCol);
                    break;
                    
                case FieldValueColumn.MultiDoubleValueColumn:
                    MultiDoubleValueColumn multiDoubleCol = new MultiDoubleValueColumn();
                    columnTable.fieldValueColumn(multiDoubleCol);
                    deserializeMultiDoubleColumn(record, name, size, multiDoubleCol);
                    break;
                    
                default:
                    break;
            }
        }
        
        return record;
    }
    
    private static void deserializeStringColumn(Record record, String name, int size, StringValueColumn col) {
        
        Column<String> column = new Column<>(size, String.class);
        for (int j = 0; j < col.valueLength(); j++) {
            column.setValue(j, col.value(j));
        }
        record.setColumn(name, column);
    }
    
    private static void deserializeInt32Column(Record record, String name, int size, Int32ValueColumn col) {
        
        Column<Integer> column = new Column<>(size, Integer.class);
        for (int j = 0; j < col.valueLength(); j++) {
            column.setValue(j, col.value(j));
        }
        record.setColumn(name, column);
    }
    
    private static void deserializeInt64Column(Record record, String name, int size, Int64ValueColumn col) {
        
        Column<Long> column = new Column<>(size, Long.class);
        for (int j = 0; j < col.valueLength(); j++) {
            column.setValue(j, col.value(j));
        }
        record.setColumn(name, column);
    }
    
    private static void deserializeFloatColumn(Record record, String name, int size, FloatValueColumn col) {
        
        Column<Float> column = new Column<>(size, Float.class);
        for (int j = 0; j < col.valueLength(); j++) {
            column.setValue(j, col.value(j));
        }
        record.setColumn(name, column);
    }
    
    private static void deserializeDoubleColumn(Record record, String name, int size, DoubleValueColumn col) {
        
        Column<Double> column = new Column<>(size, Double.class);
        for (int j = 0; j < col.valueLength(); j++) {
            column.setValue(j, col.value(j));
        }
        record.setColumn(name, column);
    }
    
    private static void deserializeBoolColumn(Record record, String name, int size, BoolValueColumn col) {
        
        Column<Boolean> column = new Column<>(size, Boolean.class);
        for (int j = 0; j < col.valueLength(); j++) {
            column.setValue(j, col.value(j));
        }
        record.setColumn(name, column);
    }
    
    private static void deserializeMultiStringColumn(Record record, String name, int size, MultiStringValueColumn col) {
        
        Column<String[]> column = new Column<>(size, String[].class);
        for (int j = 0; j < col.valueLength(); j++) {
            MultiStringValue multiValue = col.value(j);
            String[] list = new String[multiValue.valueLength()];
            for (int k = 0; k < multiValue.valueLength(); k++) {
                list[k] = multiValue.value(k);
            }
            column.setValue(j, list);
        }
        record.setColumn(name, column);
    }
    
    private static void deserializeMultiInt32Column(Record record, String name, int size, MultiInt32ValueColumn col) {
        
        Column<int[]> column = new Column<>(size, int[].class);
        for (int j = 0; j < col.valueLength(); j++) {
            MultiInt32Value multiValue = col.value(j);
            int[] list = new int[multiValue.valueLength()];
            for (int k = 0; k < multiValue.valueLength(); k++) {
                list[k] = multiValue.value(k);
            }
            column.setValue(j, list);
        }
        record.setColumn(name, column);
    }
    
    private static void deserializeMultiInt64Column(Record record, String name, int size, MultiInt64ValueColumn col) {
        
        Column<long[]> column = new Column<>(size, long[].class);
        for (int j = 0; j < col.valueLength(); j++) {
            MultiInt64Value multiValue = col.value(j);
            long[] list = new long[multiValue.valueLength()];
            for (int k = 0; k < multiValue.valueLength(); k++) {
                list[k] = multiValue.value(k);
            }
            column.setValue(j, list);
        }
        record.setColumn(name, column);
    }
    
    private static void deserializeMultiFloatColumn(Record record, String name, int size, MultiFloatValueColumn col) {
        
        Column<float[]> column = new Column<>(size, float[].class);
        for (int j = 0; j < col.valueLength(); j++) {
            MultiFloatValue multiValue = col.value(j);
            float[] list = new float[multiValue.valueLength()];
            for (int k = 0; k < multiValue.valueLength(); k++) {
                list[k] = multiValue.value(k);
            }
            column.setValue(j, list);
        }
        record.setColumn(name, column);
    }
    
    private static void deserializeMultiDoubleColumn(Record record, String name, int size, MultiDoubleValueColumn col) {
        
        Column<double[]> column = new Column<>(size, double[].class);
        for (int j = 0; j < col.valueLength(); j++) {
            MultiDoubleValue multiValue = col.value(j);
            double[] list = new double[multiValue.valueLength()];
            for (int k = 0; k < multiValue.valueLength(); k++) {
                list[k] = multiValue.value(k);
            }
            column.setValue(j, list);
        }
        record.setColumn(name, column);
    }
}
