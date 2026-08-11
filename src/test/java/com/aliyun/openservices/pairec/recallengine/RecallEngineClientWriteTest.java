package com.aliyun.openservices.pairec.recallengine;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Covers the synchronous buffered write path: batching, slicing, grouping,
 * retry classification and buffer hygiene. {@link RecallEngineClient#doWrite}
 * is stubbed out so no server is needed.
 */
public class RecallEngineClientWriteTest {

    /** One observed call to doWrite. */
    private static class Call {
        final String instanceId;
        final String table;
        final int rows;
        final InsertMode insertMode;
        final String threadName;

        Call(String instanceId, String table, int rows, InsertMode insertMode, String threadName) {
            this.instanceId = instanceId;
            this.table = table;
            this.rows = rows;
            this.insertMode = insertMode;
            this.threadName = threadName;
        }
    }

    /**
     * Client that records every write attempt instead of sending it, and can be
     * told to fail the first N attempts with a given HTTP status.
     */
    private static class RecordingClient extends RecallEngineClient {
        final List<Call> calls = Collections.synchronizedList(new ArrayList<Call>());
        volatile int failuresRemaining = 0;
        volatile int failureStatusCode = 503;

        RecordingClient() {
            // Never dialled: doWrite is overridden.
            super("http://127.0.0.1:1", "u", "p");
        }

        @Override
        WriteResponse doWrite(String instanceId, String table, WriteRequest request)
                throws RecallEngineException {
            calls.add(new Call(instanceId, table, request.getContent().size(),
                    request.getInsertMode(), Thread.currentThread().getName()));
            if (failuresRemaining > 0) {
                failuresRemaining--;
                throw new RecallEngineException("injected failure", failureStatusCode);
            }
            WriteResponse response = new WriteResponse();
            response.setCode("OK");
            return response;
        }
    }

    private static WriteRequest requestOf(int rows) {
        return requestOf(rows, InsertMode.INSERT);
    }

    private static WriteRequest requestOf(int rows, InsertMode insertMode) {
        List<Map<String, Object>> content = new ArrayList<>(rows);
        for (int i = 0; i < rows; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", String.valueOf(i));
            content.add(row);
        }
        WriteRequest request = new WriteRequest();
        request.setContent(content);
        request.setInsertMode(insertMode);
        return request;
    }

    /**
     * Rows sit in the buffer until a full batch has accumulated, and the batch
     * is then written on the caller's own thread — that inline write is what
     * paces the producer.
     */
    @Test(timeout = 10000)
    public void testFlushesOnCallerThreadWhenBatchFull() {
        RecordingClient client = new RecordingClient();
        client.withBatchSize(3).withFlushInterval(60_000);
        try {
            client.write("inst", "tbl", requestOf(2));
            assertEquals("partial batch must not be written yet", 0, client.calls.size());

            client.write("inst", "tbl", requestOf(1));
            assertEquals("full batch must be written before write() returns",
                    1, client.calls.size());
            assertEquals(3, client.calls.get(0).rows);
            assertEquals("write must happen on the calling thread, not a pool thread",
                    Thread.currentThread().getName(), client.calls.get(0).threadName);
        } finally {
            client.close();
        }
    }

    /**
     * A burst larger than one batch must be split, not coalesced into a single
     * oversized request.
     */
    @Test(timeout = 10000)
    public void testBatchIsSlicedToBatchSize() {
        RecordingClient client = new RecordingClient();
        client.withBatchSize(200).withFlushInterval(60_000);
        try {
            client.write("inst", "tbl", requestOf(500));

            assertEquals(3, client.calls.size());
            assertEquals(200, client.calls.get(0).rows);
            assertEquals(200, client.calls.get(1).rows);
            assertEquals(100, client.calls.get(2).rows);
        } finally {
            client.close();
        }
    }

    /** Rows for different tables or insert modes cannot share a request. */
    @Test(timeout = 10000)
    public void testGroupsByTableAndInsertMode() {
        RecordingClient client = new RecordingClient();
        client.withBatchSize(4).withFlushInterval(60_000);
        try {
            client.write("inst", "tbl_a", requestOf(1, InsertMode.INSERT));
            client.write("inst", "tbl_b", requestOf(1, InsertMode.INSERT));
            client.write("inst", "tbl_a", requestOf(1, InsertMode.UPSERT));
            client.write("inst", "tbl_a", requestOf(1, InsertMode.INSERT));

            assertEquals("one request per instance/table/mode group", 3, client.calls.size());
            for (Call call : client.calls) {
                if ("tbl_b".equals(call.table)) {
                    assertEquals(1, call.rows);
                } else if (call.insertMode == InsertMode.UPSERT) {
                    assertEquals(1, call.rows);
                } else {
                    assertEquals("both INSERT rows for tbl_a share one request", 2, call.rows);
                }
            }
        } finally {
            client.close();
        }
    }

    /** A 503 is transient: keep trying up to retryTimes attempts. */
    @Test(timeout = 10000)
    public void testRetriesServerErrorUntilSuccess() {
        RecordingClient client = new RecordingClient();
        client.withBatchSize(1).withFlushInterval(60_000).withRetryTimes(3);
        client.failureStatusCode = 503;
        client.failuresRemaining = 2;
        try {
            client.write("inst", "tbl", requestOf(1));

            assertEquals("two failures then one success", 3, client.calls.size());
        } finally {
            client.close();
        }
    }

    /** 429 means slow down, which a retry can still satisfy. */
    @Test(timeout = 10000)
    public void testRetriesThrottling() {
        RecordingClient client = new RecordingClient();
        client.withBatchSize(1).withFlushInterval(60_000).withRetryTimes(2);
        client.failureStatusCode = 429;
        client.failuresRemaining = 1;
        try {
            client.write("inst", "tbl", requestOf(1));

            assertEquals(2, client.calls.size());
        } finally {
            client.close();
        }
    }

    /** A malformed request will be just as malformed on the second attempt. */
    @Test(timeout = 10000)
    public void testDoesNotRetryClientError() {
        RecordingClient client = new RecordingClient();
        client.withBatchSize(1).withFlushInterval(60_000).withRetryTimes(5);
        client.failureStatusCode = 400;
        client.failuresRemaining = 5;
        try {
            client.write("inst", "tbl", requestOf(1));

            assertEquals("4xx must not be retried", 1, client.calls.size());
        } finally {
            client.close();
        }
    }

    /**
     * A failure with no response at all (connect/read timeout) is transient and
     * must be retried.
     */
    @Test(timeout = 10000)
    public void testRetriesTransportFailure() {
        RecordingClient client = new RecordingClient();
        client.withBatchSize(1).withFlushInterval(60_000).withRetryTimes(3);
        client.failureStatusCode = RecallEngineException.NO_STATUS_CODE;
        client.failuresRemaining = 2;
        try {
            client.write("inst", "tbl", requestOf(1));

            assertEquals(3, client.calls.size());
        } finally {
            client.close();
        }
    }

    /**
     * retryTimes is a total attempt count and is floored at 1, so a client left
     * at the default still sends the batch instead of dropping it.
     */
    @Test(timeout = 10000)
    public void testSendsOnceWhenRetryTimesIsZero() {
        RecordingClient client = new RecordingClient();
        client.withBatchSize(1).withFlushInterval(60_000).withRetryTimes(0);
        try {
            client.write("inst", "tbl", requestOf(1));

            assertEquals(1, client.calls.size());
        } finally {
            client.close();
        }
    }

    /**
     * A batch that exhausts its retries is dropped, not left in the buffer:
     * otherwise the buffer would grow without bound and the next flush would
     * resend rows the backend already rejected.
     */
    @Test(timeout = 10000)
    public void testFailedBatchIsNotLeftInBuffer() {
        RecordingClient client = new RecordingClient();
        client.withBatchSize(2).withFlushInterval(60_000).withRetryTimes(1);
        client.failureStatusCode = 400;
        client.failuresRemaining = Integer.MAX_VALUE;
        try {
            client.write("inst", "tbl", requestOf(2));
            assertEquals(1, client.calls.size());

            // Buffer must be empty now, so an explicit flush has nothing to send.
            client.writeFlush();
            assertEquals("failed rows must not be retained or resent",
                    1, client.calls.size());
        } finally {
            client.close();
        }
    }

    /**
     * A stream that never fills a batch must still land: the flush timer writes
     * out a partial buffer once the interval has elapsed.
     */
    @Test(timeout = 10000)
    public void testFlushTimerWritesPartialBatch() throws Exception {
        RecordingClient client = new RecordingClient();
        client.withBatchSize(10_000).withFlushInterval(30);
        try {
            client.write("inst", "tbl", requestOf(1));

            long deadline = System.currentTimeMillis() + 5000;
            while (client.calls.isEmpty() && System.currentTimeMillis() < deadline) {
                Thread.sleep(10);
            }

            assertEquals("partial batch must be flushed by the timer", 1, client.calls.size());
            assertEquals(1, client.calls.get(0).rows);
            assertTrue("timer thread should own the flush, not the producer",
                    client.calls.get(0).threadName.startsWith("RecallEngineFlushTimer-"));
        } finally {
            client.close();
        }
    }

    /** close() must not leave buffered rows behind. */
    @Test(timeout = 10000)
    public void testCloseFlushesRemainingRows() {
        RecordingClient client = new RecordingClient();
        client.withBatchSize(10_000).withFlushInterval(60_000);

        client.write("inst", "tbl", requestOf(5));
        assertEquals(0, client.calls.size());

        client.close();

        assertEquals("close() must flush the buffer", 1, client.calls.size());
        assertEquals(5, client.calls.get(0).rows);
    }
}
