package com.aliyun.openservices.pairec.recallengine;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link DeleteRequest}.
 */
public class DeleteRequestTest {

    @Test
    public void testDefaultSchemaIsApplied() {
        DeleteRequest req = new DeleteRequest(Arrays.asList("k1"));
        assertEquals(DeleteRequest.DEFAULT_SCHEMA, req.getSchema());
    }

    @Test
    public void testSetSchemaWithValidValue() {
        DeleteRequest req = new DeleteRequest();
        req.setSchema("custom");
        assertEquals("custom", req.getSchema());
    }

    @Test
    public void testSetSchemaWithNullFallsBackToDefault() {
        DeleteRequest req = new DeleteRequest();
        req.setSchema("custom");
        req.setSchema(null);
        assertEquals(
            "setSchema(null) must fall back to default schema to avoid 'null' path segments in URLs",
            DeleteRequest.DEFAULT_SCHEMA, req.getSchema());
    }

    @Test
    public void testSetSchemaWithEmptyFallsBackToDefault() {
        DeleteRequest req = new DeleteRequest();
        req.setSchema("");
        assertEquals(DeleteRequest.DEFAULT_SCHEMA, req.getSchema());
    }

    @Test
    public void testValidateRejectsNullKeys() {
        DeleteRequest req = new DeleteRequest();
        try {
            req.validate();
            fail("validate() should reject null keys");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void testValidateRejectsEmptyKeys() {
        DeleteRequest req = new DeleteRequest(Collections.<String>emptyList());
        try {
            req.validate();
            fail("validate() should reject empty keys");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void testValidateRejectsTooManyKeys() {
        List<String> keys = new ArrayList<>(DeleteRequest.MAX_DELETE_KEYS_COUNT + 1);
        for (int i = 0; i <= DeleteRequest.MAX_DELETE_KEYS_COUNT; i++) {
            keys.add("k" + i);
        }
        DeleteRequest req = new DeleteRequest(keys);
        try {
            req.validate();
            fail("validate() should reject keys exceeding the limit");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void testValidateAcceptsBoundaryKeyCount() {
        List<String> keys = new ArrayList<>(DeleteRequest.MAX_DELETE_KEYS_COUNT);
        for (int i = 0; i < DeleteRequest.MAX_DELETE_KEYS_COUNT; i++) {
            keys.add("k" + i);
        }
        DeleteRequest req = new DeleteRequest(keys);
        // should not throw
        req.validate();
    }
}
