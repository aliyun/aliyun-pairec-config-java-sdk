package com.aliyun.openservices.pairec.recallengine;

import org.junit.Test;

import static org.junit.Assert.*;

public class ColumnTest {
    
    @Test
    public void testStringColumn() {
        Column<String> column = new Column<>(5, String.class);
        
        column.setValue(0, "test1");
        column.setValue(1, "test2");
        
        assertEquals("test1", column.get(0));
        assertEquals("test2", column.get(1));
        assertEquals(5, column.size());
    }
    
    @Test
    public void testIntegerColumn() {
        Column<Integer> column = new Column<>(5, Integer.class);
        
        column.setValue(0, 100);
        column.setValue(1, 200);
        
        assertEquals(Integer.valueOf(100), column.get(0));
        assertEquals(Integer.valueOf(200), column.get(1));
    }
    
    @Test
    public void testDoubleColumn() {
        Column<Double> column = new Column<>(5, Double.class);
        
        column.setValue(0, 1.5);
        column.setValue(1, 2.5);
        
        assertEquals(Double.valueOf(1.5), column.get(0));
        assertEquals(Double.valueOf(2.5), column.get(1));
    }
    
    @Test
    public void testSafeGet() {
        Column<String> column = new Column<>(5, String.class);
        column.setValue(0, "value");
        
        assertEquals("value", column.safeGet(0));
    }
    
    @Test(expected = IndexOutOfBoundsException.class)
    public void testSetValueOutOfBounds() {
        Column<String> column = new Column<>(5, String.class);
        column.setValue(10, "test"); // 应该抛出异常
    }
    
    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetOutOfBounds() {
        Column<String> column = new Column<>(5, String.class);
        column.get(10); // 应该抛出异常
    }
    
    @Test
    public void testSetData() {
        Column<String> column = new Column<>(5, String.class);
        
        String[] data = {"a", "b", "c", "d", "e"};
        column.setData(data);
        
        assertEquals("a", column.get(0));
        assertEquals("e", column.get(4));
    }
    
    @Test
    public void testConstColumn() {
        ConstColumn<String> column = new ConstColumn<>(10, "constant");
        
        assertEquals("constant", column.get(0));
        assertEquals("constant", column.get(5));
        assertEquals("constant", column.get(9));
        assertEquals(10, column.size());
    }
    
    @Test
    public void testConstColumnSafeGet() {
        ConstColumn<Integer> column = new ConstColumn<>(5, 100);
        
        assertEquals(Integer.valueOf(100), column.safeGet(0));
        assertEquals(Integer.valueOf(100), column.safeGet(4));
    }
    
    @Test
    public void testArrayColumn() {
        Column<String[]> column = new Column<>(3, String[].class);
        
        String[] arr1 = {"a", "b", "c"};
        String[] arr2 = {"x", "y", "z"};
        
        column.setValue(0, arr1);
        column.setValue(1, arr2);
        
        assertArrayEquals(arr1, (String[]) column.get(0));
        assertArrayEquals(arr2, (String[]) column.get(1));
    }
}
