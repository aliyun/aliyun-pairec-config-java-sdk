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
        String instanceId = System.getenv("RECALL_ENGINE_INSTANCE_ID");

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
        String instanceId = System.getenv("RECALL_ENGINE_INSTANCE_ID");

        WriteRequest request = new WriteRequest();
        request.setRequestId("write-req-123");

        Map<String, Object> item = new HashMap<>();
        item.put("user_id", "123");
        item.put("item_id", "item_15");
        item.put("score", 0.16);

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

    /**
     * Test partial field update using UPSERT mode with KV table.
     * 
     * Table: item_table_test_v4 (KV table, single primary key)
     * Fields:
     *   - item_id (STRING) - primary key
     *   - bool_field (BOOL)
     *   - category (STRING)
     *   - list (ARRAY_STRING)
     */
    @Test
    public void testPartialUpdate() throws InterruptedException {
        String instanceId = System.getenv("RECALL_ENGINE_INSTANCE_ID");
        String tableName = "item_table_test_v4";

        // Step 1: Insert initial record with all fields
        WriteRequest insertRequest = new WriteRequest();
        insertRequest.setRequestId("partial-update-init");
        insertRequest.setInsertMode(InsertMode.UPSERT);

        Map<String, Object> initialData = new HashMap<>();
        initialData.put("item_id", "item_partial_test_001");
        initialData.put("bool_field", true);
        initialData.put("category", "electronics");
        initialData.put("list", java.util.Arrays.asList("tag1", "tag2", "tag3"));

        java.util.List<Map<String, Object>> content = new java.util.ArrayList<>();
        content.add(initialData);
        insertRequest.setContent(content);

        CountDownLatch initLatch = new CountDownLatch(1);
        client.write(instanceId, tableName, insertRequest, new WriteCallback() {
            @Override
            public void onSuccess(String instId, String table, WriteResponse response) {
                System.out.println("Initial insert success: " + response.getCode());
                initLatch.countDown();
            }

            @Override
            public void onError(String instId, String table, Exception e) {
                System.err.println("Initial insert failed: " + e.getMessage());
                initLatch.countDown();
            }
        });

        assertTrue("Initial insert timeout", initLatch.await(5, TimeUnit.SECONDS));

        // Step 2: Partial update - only update category field
        WriteRequest updateRequest = new WriteRequest();
        updateRequest.setRequestId("partial-update-category");
        updateRequest.setInsertMode(InsertMode.UPSERT);  // UPSERT for partial update

        Map<String, Object> partialData = new HashMap<>();
        // Must include primary key to identify the record
        partialData.put("item_id", "item_partial_test_001");
        // Only update category, other fields (bool_field, list) remain unchanged
        partialData.put("category", "home_appliances");

        java.util.List<Map<String, Object>> updateContent = new java.util.ArrayList<>();
        updateContent.add(partialData);
        updateRequest.setContent(updateContent);

        CountDownLatch updateLatch = new CountDownLatch(1);
        AtomicReference<WriteResponse> updateResponse = new AtomicReference<>();

        client.write(instanceId, tableName, updateRequest, new WriteCallback() {
            @Override
            public void onSuccess(String instId, String table, WriteResponse response) {
                System.out.println("Partial update success: category updated to 'home_appliances'");
                System.out.println("Response code: " + response.getCode());
                updateResponse.set(response);
                updateLatch.countDown();
            }

            @Override
            public void onError(String instId, String table, Exception e) {
                System.err.println("Partial update failed: " + e.getMessage());
                updateLatch.countDown();
            }
        });

        assertTrue("Partial update timeout", updateLatch.await(5, TimeUnit.SECONDS));
        assertNotNull("Update response should not be null", updateResponse.get());
        assertEquals("OK", updateResponse.get().getCode());
    }
}
