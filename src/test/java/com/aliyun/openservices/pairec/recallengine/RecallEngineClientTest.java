package com.aliyun.openservices.pairec.recallengine;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

    /**
     * Verifies that calling write() after close() fails fast instead of
     * silently dropping data: close() flushes and drains the buffer, so a row
     * added afterwards would have nothing left to write it out.
     */
    @Test(timeout = 5000)
    public void testWriteAfterCloseThrows() {
        // Endpoint is irrelevant — we never reach the network, because the
        // buffer never fills and close() flushes an empty buffer.
        RecallEngineClient cli = new RecallEngineClient("http://127.0.0.1:1", "u", "p")
                .withBatchSize(10000)
                .withFlushInterval(60_000);

        cli.close();

        WriteRequest req = new WriteRequest();
        Map<String, Object> row = new HashMap<>();
        row.put("id", "1");
        req.setContent(Collections.singletonList(row));

        try {
            cli.write("inst", "tbl", req);
            org.junit.Assert.fail("write() after close() should throw IllegalStateException");
        } catch (IllegalStateException expected) {
            assertTrue("error message should mention 'closed': " + expected.getMessage(),
                    expected.getMessage().toLowerCase().contains("closed"));
        }
    }
}
