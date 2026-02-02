package com.aliyun.openservices.pairec.recallengine;

/**
 * ConstColumn represents a column with constant value
 * 
 * @param <T> the type of data stored in this column
 */
public class ConstColumn<T> implements IColumn {
    private T data;
    private int size;
    
    public ConstColumn(int size, T data) {
        this.size = size;
        this.data = data;
    }
    
    @Override
    public Object get(int index) {
        return data;
    }
    
    public T safeGet(int index) {
        return data;
    }
    
    @Override
    public int size() {
        return size;
    }
}
