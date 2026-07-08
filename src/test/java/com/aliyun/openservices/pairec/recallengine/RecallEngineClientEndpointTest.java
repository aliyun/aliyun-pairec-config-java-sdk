package com.aliyun.openservices.pairec.recallengine;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests covering endpoint scheme normalization in {@link RecallEngineClient}.
 *
 * <p>The constructor and the public-endpoint OpenAPI fetch path must produce
 * endpoints that always carry an explicit URL scheme; otherwise OkHttp throws
 * {@code IllegalArgumentException: Expected URL scheme 'http' or 'https'}.</p>
 */
public class RecallEngineClientEndpointTest {

    private static String readEndpointField(RecallEngineClient client) throws Exception {
        Field f = RecallEngineClient.class.getDeclaredField("endpoint");
        f.setAccessible(true);
        return (String) f.get(client);
    }

    private static String invokeNormalize(String input) throws Exception {
        Method m = RecallEngineClient.class.getDeclaredMethod("normalizeEndpoint", String.class);
        m.setAccessible(true);
        return (String) m.invoke(null, input);
    }

    @Test
    public void testConstructorAddsHttpSchemeWhenMissing() throws Exception {
        RecallEngineClient client = new RecallEngineClient("example.com:1234", "u", "p");
        assertEquals("http://example.com:1234", readEndpointField(client));
    }

    @Test
    public void testConstructorPreservesHttpsScheme() throws Exception {
        RecallEngineClient client = new RecallEngineClient("https://example.com", "u", "p");
        assertEquals("https://example.com", readEndpointField(client));
    }

    @Test
    public void testConstructorPreservesHttpScheme() throws Exception {
        RecallEngineClient client = new RecallEngineClient("http://example.com", "u", "p");
        assertEquals("http://example.com", readEndpointField(client));
    }

    @Test
    public void testNormalizeEndpointWithoutScheme() throws Exception {
        assertEquals("http://pairec-public.aliyuncs.com",
            invokeNormalize("pairec-public.aliyuncs.com"));
    }

    @Test
    public void testNormalizeEndpointWithHttpsScheme() throws Exception {
        assertEquals("https://pairec-public.aliyuncs.com",
            invokeNormalize("https://pairec-public.aliyuncs.com"));
    }

    @Test
    public void testNormalizeEndpointWithHttpScheme() throws Exception {
        assertEquals("http://pairec-public.aliyuncs.com",
            invokeNormalize("http://pairec-public.aliyuncs.com"));
    }

    @Test
    public void testNormalizeEndpointWithNull() throws Exception {
        assertNull(invokeNormalize(null));
    }

    @Test
    public void testNormalizeEndpointWithEmpty() throws Exception {
        assertEquals("", invokeNormalize(""));
    }
}
