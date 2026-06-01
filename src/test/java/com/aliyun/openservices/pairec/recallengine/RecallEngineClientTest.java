package com.aliyun.openservices.pairec.recallengine;

import org.junit.Before;
import org.junit.Test;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
     * Verifies the fix for the "writeFlush holds the buffer lock while waiting
     * on HTTP" issue: concurrent producers must not be blocked by an in-flight
     * flush. Uses a localhost ServerSocket as a TCP blackhole — connections
     * are accepted but never read/written — so the OkHttp client hangs until
     * its read/write timeout fires. During that window the test issues a
     * second write() and asserts it returns promptly.
     */
    @Test(timeout = 15000)
    public void testWriteFlushDoesNotBlockProducers() throws Exception {
        try (ServerSocket blackhole = new ServerSocket(0)) {
            // Accept connections but never respond; keep the sockets alive so
            // OkHttp blocks on read/write timeout (~500ms) instead of failing
            // fast on connection-refused.
            final java.util.List<Socket> accepted = Collections.synchronizedList(new java.util.ArrayList<>());
            Thread acceptor = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        accepted.add(blackhole.accept());
                    } catch (Exception ignored) {
                        return;
                    }
                }
            }, "test-blackhole-acceptor");
            acceptor.setDaemon(true);
            acceptor.start();

            String endpoint = "http://127.0.0.1:" + blackhole.getLocalPort();
            RecallEngineClient cli = new RecallEngineClient(endpoint, "u", "p")
                    .withBatchSize(10000)         // never auto-trigger by size
                    .withFlushInterval(60_000)    // never auto-trigger by time
                    .withFlushTimeoutMs(2000);    // bound the flush wait

            try {
                // Seed one item so writeFlush has data to drain
                WriteRequest seed = new WriteRequest();
                Map<String, Object> row = new HashMap<>();
                row.put("id", "1");
                seed.setContent(Collections.singletonList(row));
                cli.write("inst", "tbl", seed);

                // Run flush in background. With the fix, it submits the HTTP
                // task while holding writeLock, then releases the lock and
                // waits on the future outside the lock.
                Thread flusher = new Thread(cli::writeFlush, "test-flusher");
                flusher.start();

                // Give flusher time to enter future.get() (lock released by now)
                Thread.sleep(100);

                // Concurrent producer must not be blocked by the in-flight HTTP.
                // Pre-fix: this call would block ~500ms (until OkHttp timeout).
                // Post-fix: only contends for the buffer lock, which is free.
                long t0 = System.nanoTime();
                WriteRequest req2 = new WriteRequest();
                Map<String, Object> row2 = new HashMap<>();
                row2.put("id", "2");
                req2.setContent(Collections.singletonList(row2));
                cli.write("inst", "tbl", req2);
                long writeElapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);

                assertTrue("write() blocked while writeFlush() was awaiting HTTP: "
                                + writeElapsedMs + "ms",
                        writeElapsedMs < 200);

                // Flush must terminate within flushTimeoutMs + slack
                flusher.join(5000);
                assertFalse("writeFlush() did not return within timeout window",
                        flusher.isAlive());
            } finally {
                cli.close();
                acceptor.interrupt();
                for (Socket s : accepted) {
                    try { s.close(); } catch (Exception ignored) {}
                }
            }
        }
    }
}
