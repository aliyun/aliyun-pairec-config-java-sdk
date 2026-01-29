package com.aliyun.openservices.pairec.recallengine;

import java.util.Arrays;

/**
 * Column represents a column of data with generic type
 * 
 * @param <T> the type of data stored in this column
 */
public class Column<T> implements IColumn {
    private T[] data;
    
    @SuppressWarnings("unchecked")
    public Column(int size, Class<T> type) {
        this.data = (T[]) java.lang.reflect.Array.newInstance(type, size);
    }
    
    public Column(T[] data) {
        this.data = Arrays.copyOf(data, data.length);
    }
    
    @Override
    public Object get(int index) {
        if (index >= data.length) {
            throw new IndexOutOfBoundsException("index out of range");
        }
        return data[index];
    }
    
    public T safeGet(int index) {
        return data[index];
    }
    
    public void setValue(int index, T value) {
        if (index >= data.length) {
            throw new IndexOutOfBoundsException("index out of range");
        }
        data[index] = value;
    }
    
    public void setData(T[] newData) {
        int size = Math.min(data.length, newData.length);
        System.arraycopy(newData, 0, data, 0, size);
    }
    
    @Override
    public int size() {
        return data.length;
    }
    
    public T[] getData() {
        return data;
    }
}
