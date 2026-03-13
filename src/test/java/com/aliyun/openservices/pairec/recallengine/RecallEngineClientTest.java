package com.aliyun.openservices.pairec.recallengine;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
        Record result = resp.getResult();
        assertEquals(100, result.size());

        // Print more record data to verify results
        System.out.println("Total records: " + result.size());
        System.out.println("Field names: " + result.fieldNames());
        System.out.println("First 5 records data: " + result.retain(5).toString());
    }


    @Test
    public void testWrite() throws RecallEngineException, InterruptedException {
        String instanceId = System.getenv("INSTANCE_ID");

        WriteRequest request = new WriteRequest();
        request.setRequestId("write-req-123");

        Map<String, Object> item = new HashMap<>();
        item.put("user_id", "123");
        item.put("item_id", "item_12");
        item.put("score", 0.96);

        java.util.List<Map<String, Object>> content = new java.util.ArrayList<>();
        content.add(item);
        request.setContent(content);

        assertNotNull(request);
        assertEquals("write-req-123", request.getRequestId());
        assertNotNull(request.getContent());
        assertEquals(1, request.getContent().size());

        // 使用 CountDownLatch 等待回调执行
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<WriteResponse> callbackResponse = new AtomicReference<>();
        AtomicReference<Exception> callbackError = new AtomicReference<>();

        WriteResponse resp = client.write(instanceId, "test_u2i", request, new WriteCallback() {
            @Override
            public void onSuccess(String instId, String table, WriteResponse response) {
                System.out.println("写入成功: " + instId + "/" + table + ", code=" + response.getCode());
                callbackResponse.set(response);
                latch.countDown();
            }

            @Override
            public void onError(String instId, String table, Exception e) {
                System.err.println("写入失败: " + instId + "/" + table + ", error=" + e.getMessage());
                callbackError.set(e);
                latch.countDown();
            }
        });

        assertEquals("write-req-123", resp.getRequestId());
        assertEquals("OK", resp.getCode());

        // 等待回调执行完成，最多 5 秒
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue("Callback not executed within 5 seconds", completed);

        // 验证回调结果
        assertNull("Write should succeed, but got error", callbackError.get());
        assertNotNull("Callback response should not be null", callbackResponse.get());
        assertEquals("OK", callbackResponse.get().getCode());
    }
}
