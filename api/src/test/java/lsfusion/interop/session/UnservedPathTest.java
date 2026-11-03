package lsfusion.interop.session;

import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

// the application server passes every path it gets to processRequest, and a path the Action API does not define (/mcp,
// /files/<operation>, /form) was answered with 500 "Something went wrong", which reads as a server failure
public class UnservedPathTest {

    private static class RecordingExec implements ExecInterface {
        private String called;

        public ExternalResponse exec(String action, ExternalRequest request) {
            called = "exec " + action;
            return null;
        }

        public ExternalResponse eval(boolean action, ExternalRequest.Param paramScript, ExternalRequest request) {
            called = action ? "eval action" : "eval";
            return null;
        }
    }

    // the application server passes the whole path as servletPath, the web server splits it into servletPath and pathInfo
    private static ExternalUtils.ResultExternalResponse post(RecordingExec exec, String servletPath, String pathInfo, String query) throws Exception {
        return (ExternalUtils.ResultExternalResponse) ExternalUtils.processRequest(exec, request -> null, new ByteArrayInputStream(new byte[0]),
                null, new String[0], new String[0], new String[0], new String[0],
                null, null, null, "http", "POST", "localhost", 7651, "", servletPath, pathInfo, query, null);
    }

    private static void assertNotFound(String servletPath, String pathInfo) throws Exception {
        RecordingExec exec = new RecordingExec();
        ExternalUtils.ResultExternalResponse response = post(exec, servletPath, pathInfo, "");
        String path = servletPath + pathInfo;
        assertNull(path, exec.called);
        assertEquals(path, 404, response.statusHttp);
        assertTrue(path, EntityUtils.toString(response.response).contains("/eval/action"));
    }

    private static void assertServed(String servletPath, String pathInfo, String query, String called) throws Exception {
        RecordingExec exec = new RecordingExec();
        post(exec, servletPath, pathInfo, query);
        assertEquals(servletPath + pathInfo + "?" + query, called, exec.called);
    }

    @Test
    public void unservedPathIsNotFound() throws Exception {
        for (String path : new String[] {"/mcp", "/files/list", "/form", "/execute", "/"})
            assertNotFound(path, "");
        assertNotFound("/eval", "/foo"); // the web server passes here only /exec/* and /eval/*
    }

    @Test
    public void actionApiPathIsServed() throws Exception {
        assertServed("/exec", "", "action=a", "exec a");
        assertServed("/exec/a", "", "", "exec a");
        assertServed("/exec", "/a", "", "exec a");
        assertServed("/eval", "", "", "eval");
        assertServed("/eval/action", "", "", "eval action");
        assertServed("/eval", "/action", "", "eval action");
    }
}
