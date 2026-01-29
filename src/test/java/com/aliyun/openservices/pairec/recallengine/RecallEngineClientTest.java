package com.aliyun.openservices.pairec.recallengine;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class RecallEngineClientTest {
    
    private RecallEngineClient client;
    
    @Before
    public void setUp() {
        // 使用测试环境配置
        client = new RecallEngineClient(
            "http://localhost:8080",
            "test_user",
            "test_password"
        );
        
        client.withRetryTimes(2)
              .withRequestHeader("X-Test-Header", "test-value");
    }
    
    @Test
    public void testClientCreation() {
        assertNotNull(client);
        assertEquals(2, client.retryTimes);
        assertNotNull(client.requestHeaders);
        assertEquals("test-value", client.requestHeaders.get("X-Test-Header"));
    }
    
    @Test
    public void testRecallRequest() {
        RecallRequest request = new RecallRequest();
        request.setInstanceId("test-instance");
        request.setService("test-service");
        request.setVersion("v1");
        request.setUid("user123");
        
        // Set recalls configuration
        Map<String, RecallConf> recalls = new HashMap<>();
        recalls.put("recall1", new RecallConf("trigger1", 100));
        request.setRecalls(recalls);
        
        // Set context parameters
        Map<String, Object> contextParams = new HashMap<>();
        contextParams.put("param1", "value1");
        request.setContextParams(contextParams);
        
        assertNotNull(request);
        assertEquals("test-instance", request.getInstanceId());
        assertEquals("user123", request.getUid());
        assertNotNull(request.getRecalls());
    }
    
    @Test
    public void testWriteRequest() {
        WriteRequest request = new WriteRequest();
        request.setRequestId("write-req-123");
        
        Map<String, Object> item = new HashMap<>();
        item.put("id", "item1");
        item.put("name", "Product 1");
        item.put("score", 0.95);
        
        java.util.List<Map<String, Object>> content = new java.util.ArrayList<>();
        content.add(item);
        request.setContent(content);
        
        assertNotNull(request);
        assertEquals("write-req-123", request.getRequestId());
        assertNotNull(request.getContent());
        assertEquals(1, request.getContent().size());
    }
    
    @Test
    public void testWithRetryTimes() {
        RecallEngineClient newClient = new RecallEngineClient("http://test.com", "user", "pass");
        RecallEngineClient result = newClient.withRetryTimes(5);
        
        assertSame(newClient, result); // 验证链式调用
        assertEquals(5, newClient.retryTimes);
    }
    
    @Test
    public void testWithRequestHeader() {
        RecallEngineClient newClient = new RecallEngineClient("http://test.com", "user", "pass");
        RecallEngineClient result = newClient.withRequestHeader("Custom-Header", "custom-value");
        
        assertSame(newClient, result); // 验证链式调用
        assertEquals("custom-value", newClient.requestHeaders.get("Custom-Header"));
    }
}
