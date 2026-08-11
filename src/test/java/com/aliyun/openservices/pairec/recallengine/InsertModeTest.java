package com.aliyun.openservices.pairec.recallengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.junit.Assert.*;

public class InsertModeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testFromValueExactMatch() {
        assertEquals(InsertMode.INSERT, InsertMode.fromValue("insert"));
        assertEquals(InsertMode.UPSERT, InsertMode.fromValue("upsert"));
    }

    @Test
    public void testFromValueIsCaseInsensitive() {
        assertEquals(InsertMode.INSERT, InsertMode.fromValue("INSERT"));
        assertEquals(InsertMode.UPSERT, InsertMode.fromValue("UpSeRt"));
    }

    @Test
    public void testFromValueTrimsSurroundingWhitespace() {
        assertEquals(InsertMode.UPSERT, InsertMode.fromValue("  upsert "));
        assertEquals(InsertMode.INSERT, InsertMode.fromValue("insert\n"));
    }

    @Test
    public void testFromValueNullFallsBackToInsert() {
        assertEquals(InsertMode.INSERT, InsertMode.fromValue(null));
    }

    @Test
    public void testFromValueRejectsUnknownValue() {
        try {
            InsertMode.fromValue("upser");
            fail("expected IllegalArgumentException for unknown insert_mode");
        } catch (IllegalArgumentException e) {
            // The message must echo the rejected value and list the supported ones
            assertTrue(e.getMessage(), e.getMessage().contains("upser"));
            assertTrue(e.getMessage(), e.getMessage().contains("insert"));
            assertTrue(e.getMessage(), e.getMessage().contains("upsert"));
        }
    }

    @Test
    public void testFromValueRejectsEmptyAndBlankValue() {
        try {
            InsertMode.fromValue("");
            fail("expected IllegalArgumentException for empty insert_mode");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            InsertMode.fromValue("   ");
            fail("expected IllegalArgumentException for blank insert_mode");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void testFromValueRejectsUpdateAlias() {
        // 'update' is not a supported alias of 'upsert' and must not degrade to INSERT
        try {
            InsertMode.fromValue("update");
            fail("expected IllegalArgumentException for unsupported alias");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void testValueAndToString() {
        assertEquals("insert", InsertMode.INSERT.getValue());
        assertEquals("upsert", InsertMode.UPSERT.getValue());
        assertEquals("insert", InsertMode.INSERT.toString());
        assertEquals("upsert", InsertMode.UPSERT.toString());
    }

    @Test
    public void testWriteRequestSerializesInsertMode() throws Exception {
        WriteRequest request = new WriteRequest();
        assertEquals(InsertMode.INSERT, request.getInsertMode());
        assertTrue(objectMapper.writeValueAsString(request).contains("\"insert_mode\":\"insert\""));

        request.setInsertMode(InsertMode.UPSERT);
        assertTrue(objectMapper.writeValueAsString(request).contains("\"insert_mode\":\"upsert\""));
    }

    @Test
    public void testWriteRequestDeserializesInsertMode() throws Exception {
        WriteRequest upsert = objectMapper.readValue(
                "{\"insert_mode\":\"upsert\"}", WriteRequest.class);
        assertEquals(InsertMode.UPSERT, upsert.getInsertMode());

        // A missing insert_mode keeps the INSERT default
        WriteRequest missing = objectMapper.readValue("{}", WriteRequest.class);
        assertEquals(InsertMode.INSERT, missing.getInsertMode());
    }

    @Test
    public void testWriteRequestSetterNormalizesNullToInsert() throws Exception {
        WriteRequest request = new WriteRequest();
        request.setInsertMode(InsertMode.UPSERT);
        request.setInsertMode(null);

        assertEquals(InsertMode.INSERT, request.getInsertMode());
        assertTrue(objectMapper.writeValueAsString(request).contains("\"insert_mode\":\"insert\""));
    }

    @Test
    public void testWriteRequestDeserializesExplicitNullAsInsert() throws Exception {
        // An explicit JSON null is routed through the setter, which normalizes it
        WriteRequest request = objectMapper.readValue(
                "{\"insert_mode\":null}", WriteRequest.class);
        assertEquals(InsertMode.INSERT, request.getInsertMode());
    }

    @Test
    public void testWriteRequestDeserializationRejectsUnknownInsertMode() {
        try {
            objectMapper.readValue("{\"insert_mode\":\"upser\"}", WriteRequest.class);
            fail("expected deserialization to fail for unknown insert_mode");
        } catch (Exception expected) {
            // expected
        }
    }
}
