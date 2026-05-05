package lsfusion.server.physics.admin.mcp;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Server-side proxy to the public lsFusion AI MCP endpoint at https://ai.lsfusion.org/mcp.
 *
 * Mirrors plugin-idea/.../com/lsfusion/mcp/RemoteMcpClient.kt: opens an MCP session via
 * `initialize`, caches the `mcp-session-id` header, then issues `tools/call` requests with it.
 * Response bodies may be plain JSON or {@code text/event-stream}; the helpers below select
 * the right reader from the upstream {@code Content-Type} so a long-lived stream cannot
 * stall us until the read timeout.
 */
public class MCPRemoteClient {

    private static final String URL_STR = "https://ai.lsfusion.org/mcp";
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private static final Object LOCK = new Object();
    private static volatile String sessionId;

    /** Calls a remote tool and returns the text from its first content item. */
    public static String callRemoteTool(String toolName, JSONObject arguments) {
        return callRemoteTool(toolName, arguments, DEFAULT_TIMEOUT_SECONDS);
    }

    public static String callRemoteTool(String toolName, JSONObject arguments, int timeoutSeconds) {
        JSONObject body = new JSONObject()
                .put("jsonrpc", "2.0")
                .put("id", 1)
                .put("method", "tools/call")
                .put("params", new JSONObject()
                        .put("name", toolName)
                        .put("arguments", arguments));

        String sid = ensureSessionId(timeoutSeconds);
        String response;
        try {
            response = doPostJson(body.toString(), timeoutSeconds, sid);
        } catch (SessionExpiredException e) {
            sid = ensureSessionId(timeoutSeconds);
            response = doPostJson(body.toString(), timeoutSeconds, sid);
        }

        String jsonText = response.trim();
        JSONObject root = new JSONObject(jsonText);
        JSONObject result = root.optJSONObject("result");
        if (result == null) {
            throw new RuntimeException("Remote MCP response missing 'result': " + truncate(jsonText));
        }
        JSONArray content = result.optJSONArray("content");
        if (content == null || content.length() == 0) {
            throw new RuntimeException("Remote MCP response missing 'content'");
        }
        String text = content.getJSONObject(0).optString("text", null);
        if (text == null) {
            throw new RuntimeException("Remote MCP response missing 'text'");
        }
        if (text.startsWith("Error executing tool")) {
            throw new RuntimeException(text);
        }
        return text;
    }

    private static String ensureSessionId(int timeoutSeconds) {
        String existing = sessionId;
        if (existing != null && !existing.isEmpty()) return existing;
        synchronized (LOCK) {
            if (sessionId != null && !sessionId.isEmpty()) return sessionId;

            JSONObject body = new JSONObject()
                    .put("jsonrpc", "2.0")
                    .put("id", 1)
                    .put("method", "initialize")
                    .put("params", new JSONObject()
                            .put("protocolVersion", "2024-11-05")
                            .put("capabilities", new JSONObject())
                            .put("clientInfo", new JSONObject()
                                    .put("name", "lsfusion-server")
                                    .put("version", "0")));

            HttpURLConnection conn = null;
            try {
                conn = openConnection(timeoutSeconds, null);
                writeBody(conn, body.toString());
                int code = conn.getResponseCode();
                if (code < 200 || code >= 300) {
                    throw new RuntimeException("Remote MCP initialize failed with " + code + ": " + readError(conn));
                }
                // Drain via the same SSE-aware reader: a server that answers initialize over
                // text/event-stream and keeps the channel open would otherwise block readAll
                // until the read timeout.
                readSuccessfulBody(conn);
                String headerSession = conn.getHeaderField("mcp-session-id");
                if (headerSession == null || headerSession.isEmpty()) {
                    throw new RuntimeException("Remote MCP initialize did not return 'mcp-session-id'");
                }
                sessionId = headerSession;
                return headerSession;
            } catch (IOException e) {
                throw new RuntimeException("Remote MCP initialize I/O error: " + e.getMessage(), e);
            } finally {
                if (conn != null) conn.disconnect();
            }
        }
    }

    private static String doPostJson(String body, int timeoutSeconds, String session) {
        HttpURLConnection conn = null;
        try {
            conn = openConnection(timeoutSeconds, session);
            writeBody(conn, body);
            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                return readSuccessfulBody(conn);
            }
            String err = readError(conn);
            // Only 401 should reset the cached session id and trigger a re-initialize. 400
            // means the upstream parsed the request and rejected it (bad JSON-RPC, bad tool
            // args, etc.); retrying with a fresh session won't fix that, and surfaces a
            // misleading "session expired" diagnostic.
            if (code == 401) {
                synchronized (LOCK) {
                    if (session != null && session.equals(sessionId)) {
                        sessionId = null;
                    }
                }
                throw new SessionExpiredException("Remote MCP HTTP " + code + ": " + truncate(err));
            }
            throw new RuntimeException("Remote MCP HTTP " + code + ": " + truncate(err));
        } catch (IOException e) {
            throw new RuntimeException("Remote MCP I/O error: " + e.getMessage(), e);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static HttpURLConnection openConnection(int timeoutSeconds, String session) throws IOException {
        int safeTimeout = Math.max(1, timeoutSeconds) * 1000;
        URL url = new URL(URL_STR);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(safeTimeout);
        conn.setReadTimeout(safeTimeout);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json, text/event-stream");
        if (session != null) {
            conn.setRequestProperty("mcp-session-id", session);
        }
        return conn;
    }

    private static void writeBody(HttpURLConnection conn, String body) throws IOException {
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String readAll(InputStream in) throws IOException {
        if (in == null) return "";
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int n;
            while ((n = r.read(buf)) > 0) sb.append(buf, 0, n);
            return sb.toString();
        }
    }

    private static String readError(HttpURLConnection conn) {
        try {
            return readAll(conn.getErrorStream());
        } catch (Exception ignored) {
            return "";
        }
    }

    private static boolean isSseContentType(String contentType) {
        if (contentType == null) return false;
        // Matches `text/event-stream` and `text/event-stream; charset=utf-8`, case-insensitive.
        return contentType.toLowerCase(java.util.Locale.ROOT).contains("text/event-stream");
    }

    /**
     * Read SSE frames until the first {@code data:} event terminates (blank line per spec),
     * concatenating multi-line {@code data:} payloads with newlines as the spec requires.
     * Returns the JSON-RPC body that was carried in that event. Other field names
     * ({@code event:}, {@code id:}, {@code retry:}, comments) are ignored. Stops early at
     * EOF too, so a server that closes the connection mid-event still surfaces what arrived.
     */
    private static String readFirstSseEvent(InputStream in) throws IOException {
        if (in == null) return "";
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder data = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            if (line.isEmpty()) {
                // Blank line terminates the current event. If we already have data, return it;
                // otherwise it was an inter-event blank — keep reading for the next one.
                if (data.length() > 0) return data.toString();
                continue;
            }
            if (line.startsWith("data:")) {
                String content = line.substring(5);
                if (content.startsWith(" ")) content = content.substring(1);
                if (data.length() > 0) data.append('\n');
                data.append(content);
            }
            // Other fields (event:, id:, retry:, comments starting with ":") are ignored —
            // we only carry MCP JSON-RPC payloads here.
        }
        return data.toString();
    }

    /**
     * Drain a 2xx response body with the right reader for its {@code Content-Type}. SSE
     * responses go through {@link #readFirstSseEvent} so a long-lived stream can't stall us;
     * everything else is read whole. Shared by initialize and tools/call so they stay in sync.
     */
    private static String readSuccessfulBody(HttpURLConnection conn) throws IOException {
        if (isSseContentType(conn.getContentType())) {
            return readFirstSseEvent(conn.getInputStream());
        }
        return readAll(conn.getInputStream());
    }

    private static String truncate(String s) {
        return s.length() > 2000 ? s.substring(0, 2000) : s;
    }

    private static class SessionExpiredException extends RuntimeException {
        SessionExpiredException(String msg) { super(msg); }
    }
}
