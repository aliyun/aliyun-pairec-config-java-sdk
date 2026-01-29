package com.aliyun.openservices.pairec.recallengine;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RecallEngineClientTest {

    private RecallEngineClient client;

    @Before
    public void setUp() {
        String endpoint = System.getenv("RECALL_ENGINE_SERVICE_ENDPOINT");
        String username = System.getenv("RECALL_ENGINE_SERVICE_USERNAME");
        String password = System.getenv("RECALL_ENGINE_SERVICE_PASSWORD");
        String token = System.getenv("RECALL_ENGINE_SERVICE_TOKEN");

        // 使用测试环境配置
        client = new RecallEngineClient(endpoint, username, password);

        client.withRetryTimes(2).withRequestHeader("Authorization", token);

        assertNotNull(client);
    }

    @Test
    public void testRecall() throws RecallEngineException {
        String instanceId = System.getenv("INSTANCE_ID");

        RecallRequest request = new RecallRequest();
        request.setInstanceId(instanceId);
        request.setService("recall_test");
        request.setVersion("V1");
        request.setUid("123");

        // Set recalls configuration
        Map<String, RecallConf> recalls = new HashMap<>();
        recalls.put("u2i_recall", new RecallConf("123", 100));
        request.setRecalls(recalls);

        RecallResponse resp = client.recall(request);
        assertEquals(100, resp.getResult().size());
    }


    @Test
    public void testWrite() throws RecallEngineException {
        String instanceId = System.getenv("INSTANCE_ID");

        WriteRequest request = new WriteRequest();
        request.setRequestId("write-req-123");

        Map<String, Object> item = new HashMap<>();
        item.put("user_id", "123");
        item.put("item_id", "item_123");
        item.put("score", 0.95);

        java.util.List<Map<String, Object>> content = new java.util.ArrayList<>();
        content.add(item);
        request.setContent(content);

        assertNotNull(request);
        assertEquals("write-req-123", request.getRequestId());
        assertNotNull(request.getContent());
        assertEquals(1, request.getContent().size());

        WriteResponse resp = client.write(instanceId, "u2i_table", request);
        assertEquals("write-req-123", resp.getRequestId());
        assertEquals("OK", resp.getCode());
    }
}
