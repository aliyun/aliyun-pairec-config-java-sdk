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
            Table unionTable = new Table();
            columnTable.fieldValueColumn(unionTable);
            
            switch (columnType) {
                case FieldValueColumn.StringValueColumn:
                    deserializeStringColumn(record, name, size, unionTable);
                    break;
                    
                case FieldValueColumn.Int32ValueColumn:
                    deserializeInt32Column(record, name, size, unionTable);
                    break;
                    
                case FieldValueColumn.Int64ValueColumn:
                    deserializeInt64Column(record, name, size, unionTable);
                    break;
                    
                case FieldValueColumn.FloatValueColumn:
                    deserializeFloatColumn(record, name, size, unionTable);
                    break;
                    
                case FieldValueColumn.DoubleValueColumn:
                    deserializeDoubleColumn(record, name, size, unionTable);
                    break;
                    
                case FieldValueColumn.BoolValueColumn:
                    deserializeBoolColumn(record, name, size, unionTable);
                    break;
                    
                case FieldValueColumn.MultiStringValueColumn:
                    deserializeMultiStringColumn(record, name, size, unionTable);
                    break;
                    
                case FieldValueColumn.MultiInt32ValueColumn:
                    deserializeMultiInt32Column(record, name, size, unionTable);
                    break;
                    
                case FieldValueColumn.MultiInt64ValueColumn:
                    deserializeMultiInt64Column(record, name, size, unionTable);
                    break;
                    
                case FieldValueColumn.MultiFloatValueColumn:
                    deserializeMultiFloatColumn(record, name, size, unionTable);
                    break;
                    
                case FieldValueColumn.MultiDoubleValueColumn:
                    deserializeMultiDoubleColumn(record, name, size, unionTable);
                    break;
                    
                default:
                    break;
            }
        }
        
        return record;
    }
    
    private static void deserializeStringColumn(Record record, String name, int size, Table table) {
        StringValueColumn col = (StringValueColumn) table;
        
        Column<String> column = new Column<>(size, String.class);
        for (int j = 0; j < col.valueLength(); j++) {
            column.setValue(j, col.value(j));
        }
        record.setColumn(name, column);
    }
    
    private static void deserializeInt32Column(Record record, String name, int size, Table table) {
        Int32ValueColumn col = (Int32ValueColumn) table;
        
        Column<Integer> column = new Column<>(size, Integer.class);
        for (int j = 0; j < col.valueLength(); j++) {
            column.setValue(j, col.value(j));
        }
        record.setColumn(name, column);
    }
    
    private static void deserializeInt64Column(Record record, String name, int size, Table table) {
        Int64ValueColumn col = (Int64ValueColumn) table;
        
        Column<Long> column = new Column<>(size, Long.class);
        for (int j = 0; j < col.valueLength(); j++) {
            column.setValue(j, col.value(j));
        }
        record.setColumn(name, column);
    }
    
    private static void deserializeFloatColumn(Record record, String name, int size, Table table) {
        FloatValueColumn col = (FloatValueColumn) table;
        
        Column<Float> column = new Column<>(size, Float.class);
        for (int j = 0; j < col.valueLength(); j++) {
            column.setValue(j, col.value(j));
        }
        record.setColumn(name, column);
    }
    
    private static void deserializeDoubleColumn(Record record, String name, int size, Table table) {
        DoubleValueColumn col = (DoubleValueColumn) table;
        
        Column<Double> column = new Column<>(size, Double.class);
        for (int j = 0; j < col.valueLength(); j++) {
            column.setValue(j, col.value(j));
        }
        record.setColumn(name, column);
    }
    
    private static void deserializeBoolColumn(Record record, String name, int size, Table table) {
        BoolValueColumn col = (BoolValueColumn) table;
        
        Column<Boolean> column = new Column<>(size, Boolean.class);
        for (int j = 0; j < col.valueLength(); j++) {
            column.setValue(j, col.value(j));
        }
        record.setColumn(name, column);
    }
    
    private static void deserializeMultiStringColumn(Record record, String name, int size, Table table) {
        MultiStringValueColumn col = (MultiStringValueColumn) table;
        
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
    
    private static void deserializeMultiInt32Column(Record record, String name, int size, Table table) {
        MultiInt32ValueColumn col = (MultiInt32ValueColumn) table;
        
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
    
    private static void deserializeMultiInt64Column(Record record, String name, int size, Table table) {
        MultiInt64ValueColumn col = (MultiInt64ValueColumn) table;
        
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
    
    private static void deserializeMultiFloatColumn(Record record, String name, int size, Table table) {
        MultiFloatValueColumn col = (MultiFloatValueColumn) table;
        
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
    
    private static void deserializeMultiDoubleColumn(Record record, String name, int size, Table table) {
        MultiDoubleValueColumn col = (MultiDoubleValueColumn) table;
        
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
