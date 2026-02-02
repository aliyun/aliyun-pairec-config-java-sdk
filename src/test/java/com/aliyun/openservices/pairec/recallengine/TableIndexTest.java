package com.aliyun.openservices.pairec.recallengine;

import org.junit.Test;

import static org.junit.Assert.*;

public class TableIndexTest {
    
    @Test
    public void testCreation() {
        TableIndex index = new TableIndex(10);
        
        assertEquals(10, index.getRowCount());
        assertEquals(0, index.getIndex(0));
        assertEquals(9, index.getIndex(9));
    }
    
    @Test
    public void testSafeGet() {
        TableIndex index = new TableIndex(5);
        
        assertEquals(0, index.safeGet(0));
        assertEquals(4, index.safeGet(4));
    }
    
    @Test
    public void testMarkRemoved() {
        TableIndex index = new TableIndex(5);
        
        assertFalse(index.isRemoved(0));
        
        index.markRemoved(0);
        assertTrue(index.isRemoved(0));
    }
    
    @Test
    public void testEndRemove() {
        TableIndex index = new TableIndex(5);
        
        index.markRemoved(1);
        index.markRemoved(3);
        
        index.endRemove();
        
        assertEquals(3, index.getRowCount()); // 5 - 2 = 3
    }
    
    @Test
    public void testRebuild() {
        TableIndex index = new TableIndex(5);
        
        int[] newIndexes = {4, 3, 2, 1, 0};
        index.rebuild(5, newIndexes);
        
        assertEquals(5, index.getRowCount());
        assertEquals(4, index.getIndex(0));
        assertEquals(0, index.getIndex(4));
    }
    
    @Test
    public void testTruncate() {
        TableIndex index = new TableIndex(10);
        
        index.truncate(5);
        
        assertEquals(5, index.getRowCount());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testTruncateInvalid() {
        TableIndex index = new TableIndex(10);
        index.truncate(0); // 应该抛出异常
    }
    
    @Test
    public void testTruncateFrom() {
        TableIndex index = new TableIndex(10);
        
        index.truncateFrom(5, 3); // 从第5个开始，保留3个
        
        assertEquals(3, index.getRowCount());
        assertEquals(5, index.getIndex(0));
    }
    
    @Test
    public void testGetIndexes() {
        TableIndex index = new TableIndex(5);
        
        int[] indexes = index.getIndexes();
        
        assertNotNull(indexes);
        assertEquals(5, indexes.length);
        assertEquals(0, indexes[0]);
        assertEquals(4, indexes[4]);
    }
    
    @Test
    public void testMaxSize() {
        // 测试超过最大值的情况
        TableIndex index = new TableIndex(TableIndex.DOC_INDEX_MASK + 100);
        
        assertEquals(TableIndex.DOC_INDEX_MASK, index.getRowCount());
    }
    
    @Test
    public void testNegativeSize() {
        // 测试负数大小
        TableIndex index = new TableIndex(-5);
        
        assertEquals(0, index.getRowCount());
    }
}
