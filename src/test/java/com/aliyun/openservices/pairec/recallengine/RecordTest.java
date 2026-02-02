package com.aliyun.openservices.pairec.recallengine;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class RecordTest {
    
    private Record record;
    
    @Before
    public void setUp() {
        record = new Record(10);
        
        // 添加测试数据
        Column<String> idColumn = new Column<>(10, String.class);
        Column<Double> scoreColumn = new Column<>(10, Double.class);
        Column<Integer> rankColumn = new Column<>(10, Integer.class);
        
        for (int i = 0; i < 10; i++) {
            idColumn.setValue(i, "item" + i);
            scoreColumn.setValue(i, (10 - i) * 0.1); // 降序分数
            rankColumn.setValue(i, i + 1);
        }
        
        record.setColumn("item_id", idColumn);
        record.setColumn("score", scoreColumn);
        record.setColumn("rank", rankColumn);
    }
    
    @Test
    public void testRecordSize() {
        assertEquals(10, record.size());
        assertEquals(10, record.len());
    }
    
    @Test
    public void testGetColumn() {
        IColumn column = record.getColumn("item_id");
        assertNotNull(column);
        assertEquals("item0", column.get(0));
    }
    
    @Test
    public void testSort() {
        Record result = record.sort("score", false); // 升序
        
        assertSame(record, result); // 验证链式调用
        
        List<Object> values = record.columnValues("score");
        assertTrue((Double) values.get(0) < (Double) values.get(1));
    }
    
    @Test
    public void testSortDescending() {
        Record result = record.sort("score", true); // 降序
        
        assertSame(record, result);
        
        List<Object> values = record.columnValues("score");
        assertTrue((Double) values.get(0) > (Double) values.get(1));
    }
    
    @Test
    public void testRetain() {
        Record result = record.retain(5);
        
        assertSame(record, result); // 验证链式调用
        assertEquals(5, record.size());
    }
    
    @Test
    public void testFilter() {
        // 添加重复数据
        Column<String> column = (Column<String>) record.getColumn("item_id");
        column.setValue(5, "item0"); // 创建重复
        
        Record result = record.filter("item_id");
        
        assertSame(record, result); // 验证链式调用
        assertTrue(record.size() < 10); // 应该过滤掉重复项
    }
    
    @Test
    public void testFilterByColumnValue() {
        Record result = record.filterByColumnValue("rank", value -> {
            Integer rank = (Integer) value;
            return rank <= 5;
        });
        
        assertSame(record, result);
        assertEquals(5, record.size());
    }
    
    @Test
    public void testColumnValues() {
        List<Object> values = record.columnValues("item_id");
        
        assertNotNull(values);
        assertEquals(10, values.size());
        assertEquals("item0", values.get(0));
    }
    
    @Test
    public void testColumnValuesString() {
        List<String> values = record.columnValuesString("item_id");
        
        assertNotNull(values);
        assertEquals(10, values.size());
        assertEquals("item0", values.get(0));
    }
    
    @Test
    public void testRandom() {
        List<Object> beforeShuffle = record.columnValues("item_id");
        
        Record result = record.random();
        
        assertSame(record, result); // 验证链式调用
        assertEquals(10, record.size()); // 大小不变
        
        List<Object> afterShuffle = record.columnValues("item_id");
        assertEquals(10, afterShuffle.size());
        // 注意：不能保证打乱后一定不同，但大小应该相同
    }
    
    @Test
    public void testFieldNames() {
        List<String> fieldNames = record.fieldNames();
        
        assertNotNull(fieldNames);
        assertEquals(3, fieldNames.size());
        assertTrue(fieldNames.contains("item_id"));
        assertTrue(fieldNames.contains("score"));
        assertTrue(fieldNames.contains("rank"));
    }
    
    @Test
    public void testMethodChaining() {
        // 测试完整的链式调用
        Record result = record
            .filter("item_id")
            .sort("score", true)
            .retain(5);
        
        assertSame(record, result);
        assertEquals(5, record.size());
    }
    
    @Test
    public void testToString() {
        String str = record.toString();
        
        assertNotNull(str);
        assertTrue(str.contains("count:10"));
        assertTrue(str.contains("item_id"));
        assertTrue(str.contains("score"));
    }
}
