package com.aliyun.openservices.pairec.recallengine;

import java.util.*;
import java.util.function.Predicate;

/**
 * Record is not concurrency safe
 * Represents a collection of columnar data with indexing support
 */
public class Record {
    private Map<String, IColumn> columnData;
    private TableIndex tableIndex;
    private Map<Integer, Map<String, Object>> cacheData;
    
    public Record(int size) {
        this.columnData = new HashMap<>();
        this.tableIndex = new TableIndex(size);
    }
    
    public TableIndex getTableIndex() {
        return tableIndex;
    }
    
    public int size() {
        return tableIndex.getRowCount();
    }
    
    public void setColumn(String name, IColumn column) {
        columnData.put(name, column);
    }
    
    public IColumn getColumn(String name) {
        return columnData.get(name);
    }
    
    /**
     * Sort the record by a column
     * 
     * @param name column name to sort by
     * @param desc true for descending, false for ascending
     * @return this Record for method chaining
     */
    public Record sort(String name, boolean desc) {
        IColumn column = columnData.get(name);
        if (column == null) {
            return this;
        }
        
        int size = tableIndex.getRowCount();
        List<SortItem> sortItems = new ArrayList<>(size);
        
        for (int i = 0; i < size; i++) {
            int index = tableIndex.safeGet(i);
            Object value = column.get(index);
            sortItems.add(new SortItem(index, value));
        }
        
        sortItems.sort((a, b) -> {
            int result = compareValues(a.value, b.value);
            return desc ? -result : result;
        });
        
        int[] indexes = new int[sortItems.size()];
        for (int i = 0; i < sortItems.size(); i++) {
            indexes[i] = sortItems.get(i).index;
        }
        
        tableIndex.rebuild(indexes.length, indexes);
        return this;
    }
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    private int compareValues(Object a, Object b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        
        if (a instanceof Comparable && b instanceof Comparable) {
            return ((Comparable) a).compareTo(b);
        }
        return 0;
    }
    
    public int len() {
        return tableIndex.getRowCount();
    }
    
    /**
     * Retain only the first count elements
     * 
     * @param count number of elements to retain
     * @return this Record for method chaining
     */
    public Record retain(int count) {
        tableIndex.truncate(count);
        return this;
    }
    
    /**
     * Filter duplicate values based on a column
     * 
     * @param name column name to filter by
     * @return this Record for method chaining
     */
    public Record filter(String name) {
        IColumn column = columnData.get(name);
        if (column == null) {
            return this;
        }
        
        int size = tableIndex.getRowCount();
        int removed = 0;
        Set<String> uniqueSet = new HashSet<>(size);
        
        for (int i = 0; i < size; i++) {
            int recordIndex = tableIndex.safeGet(i);
            Object value = column.get(recordIndex);
            String strValue = String.valueOf(value);
            
            if (uniqueSet.contains(strValue)) {
                tableIndex.markRemoved(i);
                removed++;
            } else {
                uniqueSet.add(strValue);
            }
        }
        
        if (removed > 0) {
            tableIndex.endRemove();
        }
        
        return this;
    }
    
    /**
     * Filter records based on column value predicate
     * 
     * @param name column name
     * @param predicate filter predicate
     * @return this Record for method chaining
     */
    public Record filterByColumnValue(String name, Predicate<Object> predicate) {
        IColumn column = columnData.get(name);
        if (column == null) {
            return this;
        }
        
        int size = tableIndex.getRowCount();
        int removed = 0;
        
        for (int i = 0; i < size; i++) {
            int recordIndex = tableIndex.safeGet(i);
            Object value = column.get(recordIndex);
            
            if (!predicate.test(value)) {
                tableIndex.markRemoved(i);
                removed++;
            }
        }
        
        if (removed > 0) {
            tableIndex.endRemove();
        }
        
        return this;
    }
    
    /**
     * Filter records based on all column values
     * 
     * @param predicate filter predicate that receives a map of all column values
     * @return this Record for method chaining
     */
    public Record filterByValues(Predicate<Map<String, Object>> predicate) {
        int size = tableIndex.getRowCount();
        int removed = 0;
        
        if (cacheData == null) {
            cacheData = new HashMap<>(size);
        }
        
        for (int i = 0; i < size; i++) {
            int recordIndex = tableIndex.safeGet(i);
            Map<String, Object> rowData = cacheData.get(recordIndex);
            
            if (rowData == null) {
                rowData = new HashMap<>(columnData.size());
                for (Map.Entry<String, IColumn> entry : columnData.entrySet()) {
                    rowData.put(entry.getKey(), entry.getValue().get(recordIndex));
                }
                cacheData.put(recordIndex, rowData);
            }
            
            if (!predicate.test(rowData)) {
                tableIndex.markRemoved(i);
                removed++;
            }
        }
        
        if (removed > 0) {
            tableIndex.endRemove();
        }
        
        return this;
    }
    
    /**
     * Get all values from a column
     * 
     * @param columnName the column name
     * @return list of values
     */
    public List<Object> columnValues(String columnName) {
        IColumn column = columnData.get(columnName);
        if (column == null) {
            return null;
        }
        
        int size = tableIndex.getRowCount();
        List<Object> result = new ArrayList<>(size);
        
        for (int i = 0; i < size; i++) {
            int recordIndex = tableIndex.safeGet(i);
            result.add(column.get(recordIndex));
        }
        
        return result;
    }
    
    /**
     * Get all string values from a column
     * 
     * @param columnName the column name
     * @return list of string values
     */
    public List<String> columnValuesString(String columnName) {
        IColumn column = columnData.get(columnName);
        if (column == null) {
            return null;
        }
        
        int size = tableIndex.getRowCount();
        List<String> result = new ArrayList<>(size);
        
        for (int i = 0; i < size; i++) {
            int recordIndex = tableIndex.safeGet(i);
            Object value = column.get(recordIndex);
            if (value != null) {
                String strValue = String.valueOf(value);
                if (!strValue.isEmpty()) {
                    result.add(strValue);
                }
            }
        }
        
        return result;
    }
    
    /**
     * Shuffle the record randomly
     * 
     * @return this Record for method chaining
     */
    public Record random() {
        int[] indexes = tableIndex.getIndexes();
        Random random = new Random();
        
        for (int i = indexes.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = indexes[i];
            indexes[i] = indexes[j];
            indexes[j] = temp;
        }
        
        tableIndex.rebuild(indexes.length, indexes);
        return this;
    }
    
    /**
     * Get all field names
     * 
     * @return list of field names
     */
    public List<String> fieldNames() {
        return new ArrayList<>(columnData.keySet());
    }
    
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        int size = tableIndex.getRowCount();
        
        buf.append("count:").append(size).append("\t");
        
        List<String> fieldNames = new ArrayList<>(columnData.keySet());
        buf.append("names:").append(fieldNames).append("\t");
        
        for (String name : fieldNames) {
            IColumn column = getColumn(name);
            if (column == null) {
                continue;
            }
            
            List<Object> values = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                int index = tableIndex.safeGet(i);
                values.add(column.get(index));
            }
            
            buf.append(name).append(":").append(values).append("\t");
        }
        
        return buf.toString();
    }
    
    private static class SortItem {
        int index;
        Object value;
        
        SortItem(int index, Object value) {
            this.index = index;
            this.value = value;
        }
    }
}
