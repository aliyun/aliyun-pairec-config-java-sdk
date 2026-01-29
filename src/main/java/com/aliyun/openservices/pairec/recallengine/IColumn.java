package com.aliyun.openservices.pairec.recallengine;

/**
 * IColumn interface for column data
 */
public interface IColumn {
    /**
     * Get the value at the given index
     * 
     * @param index the index of the value
     * @return the value at the index
     * @throws IndexOutOfBoundsException if index is out of range
     */
    Object get(int index);
    
    /**
     * Get the size of this column
     * 
     * @return the size
     */
    int size();
}
