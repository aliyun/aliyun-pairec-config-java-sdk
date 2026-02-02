package com.aliyun.openservices.pairec.recallengine;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TableIndex maintains data positions for Record.
 * It uses an integer array to hold data positions.
 * When data is written to Record, its data position in IColumn is fixed.
 * When sorting or deleting data, the data in IColumn is not affected, but we adjust TableIndex.
 */
public class TableIndex {
    // DocIndexMask, row count should be less than 16777215 (0x00ffffff)
    public static final int DOC_INDEX_MASK = 0x00ffffff;
    
    // DocDeleteMask used to mark a row as deleted
    public static final int DOC_DELETE_MASK = 0x80000000;
    
    private final AtomicInteger rowCount;
    private int[] indexes;
    
    /**
     * Create and initialize a new TableIndex
     * 
     * @param count initial row count
     */
    public TableIndex(int count) {
        if (count > DOC_INDEX_MASK) {
            count = DOC_INDEX_MASK;
        }
        if (count < 0) {
            count = 0;
        }
        
        this.rowCount = new AtomicInteger(count);
        this.indexes = new int[count];
        
        for (int i = 0; i < count; i++) {
            this.indexes[i] = i & DOC_INDEX_MASK;
        }
    }
    
    /**
     * Get the current row count
     * 
     * @return current row count
     */
    public int getRowCount() {
        return rowCount.get();
    }
    
    /**
     * Get a copy of the internal indexes array
     * 
     * @return a copy of indexes
     */
    public int[] getIndexes() {
        return Arrays.copyOf(indexes, indexes.length);
    }
    
    /**
     * Get the actual position in IColumn for the given row
     * 
     * @param row the row number
     * @return the actual position
     */
    public int getIndex(int row) {
        return indexes[row] & DOC_INDEX_MASK;
    }
    
    /**
     * Safe get without bounds checking
     * 
     * @param row the row number
     * @return the actual position
     */
    public int safeGet(int row) {
        return indexes[row] & DOC_INDEX_MASK;
    }
    
    /**
     * Mark a row as removed
     * 
     * @param index the index to mark as removed
     */
    public void markRemoved(int index) {
        indexes[index] |= DOC_DELETE_MASK;
    }
    
    /**
     * Check if a row is marked as removed
     * 
     * @param row the row to check
     * @return true if removed, false otherwise
     */
    public boolean isRemoved(int row) {
        return (indexes[row] & DOC_DELETE_MASK) != 0;
    }
    
    /**
     * Rebuild the index with a new array
     * 
     * @param newRowCount the new row count
     * @param newIndexes the new indexes
     */
    public void rebuild(int newRowCount, int[] newIndexes) {
        this.indexes = newIndexes;
        this.rowCount.set(newRowCount);
    }
    
    /**
     * Truncate to keep only the first n rows
     * 
     * @param n number of rows to keep
     * @throws IllegalArgumentException if n is invalid
     */
    public void truncate(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("illegal table index truncate n: " + n);
        }
        truncateFrom(0, n);
    }
    
    /**
     * Truncate from a specific position, keeping n rows
     * 
     * @param from starting position
     * @param n number of rows to keep
     * @throws IllegalArgumentException if parameters are invalid
     */
    public void truncateFrom(int from, int n) {
        int currentCount = rowCount.get();
        
        if (from >= currentCount) {
            indexes = new int[0];
            rowCount.set(0);
            return;
        }
        
        if (n < 0 || from < 0) {
            throw new IllegalArgumentException("illegal table index truncate from: " + from + ", n: " + n);
        }
        
        if (n > currentCount - from) {
            n = currentCount - from;
        }
        
        int[] newIndexes = new int[n];
        System.arraycopy(indexes, from, newIndexes, 0, n);
        indexes = newIndexes;
        rowCount.set(n);
    }
    
    /**
     * Compact the array, removing all rows marked as deleted
     */
    public void endRemove() {
        int currentCount = rowCount.get();
        int writeIndex = 0;
        
        for (int readIndex = 0; readIndex < currentCount; readIndex++) {
            if ((indexes[readIndex] & DOC_DELETE_MASK) == 0) {
                if (writeIndex != readIndex) {
                    indexes[writeIndex] = indexes[readIndex];
                }
                writeIndex++;
            }
        }
        
        indexes = Arrays.copyOf(indexes, writeIndex);
        rowCount.set(writeIndex);
    }
    
    /**
     * Get the raw value at index i (including delete mark)
     * 
     * @param i the index
     * @return the raw value
     */
    public int get(int i) {
        return indexes[i];
    }
}
