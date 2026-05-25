package lsfusion.http.controller;

import lsfusion.base.file.NamedFileData;
import lsfusion.gwt.server.FileUtils;
import lsfusion.http.authentication.LSFAuthenticationToken;
import lsfusion.http.controller.oauth.OAuthRequestHandlerBase;
import lsfusion.http.provider.logics.LogicsProvider;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.connection.ConnectionInfo;
import lsfusion.interop.logics.remote.MCPResult;
import lsfusion.interop.oauth.OAuthOperations;
import lsfusion.interop.session.ExternalRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP face of the MCP (Model Context Protocol) endpoint, mounted at {@code /mcp}.
 *
 * <p>Forwards the raw JSON-RPC body to the application server via
 * {@link lsfusion.interop.logics.remote.RemoteLogicsInterface#mcp}, which dispatches through
 * {@code MCPDispatcher} server-side. The HTTP request's metadata (headers, cookies, scheme,
 * host, contextPath, sessionId, body, etc.) is captured into an {@link ExternalRequest}
 * envelope — same shape {@code /eval} builds — so scripts running through
 * {@code lsfusion_eval} can read those attributes via standard lsFusion properties
 * ({@code headers[name]} / {@code cookies[name]} / etc.).
 *
 * <p>Auth: pulls the caller's {@link AuthenticationToken} via the existing
 * {@link LSFAuthenticationToken#getAppServerToken} flow. The {@code /mcp} security chain
 * runs http-basic + bearer-token filter + URL-auth filter (no session cookie path —
 * {@code create-session=never}), so all valid auth comes from {@code Authorization} header
 * or query params. Anonymous = no valid auth on the request; we treat that as the
 * discovery-trigger condition and respond 401 + {@code WWW-Authenticate}, redirecting MCP
 * clients into the OAuth flow at {@code /.well-known/oauth-protected-resource}.
 *
 * <p>Body size: the handler enforces {@link #MAX_BODY_BYTES}; a request whose
 * {@code Content-Length} header exceeds the cap, or that streams past the cap, gets a 413
 * response. This bounds memory use ahead of any per-tool DoS guards.
 *
 * <p>Transport: only POST is meaningful here. JSON-RPC <em>notifications</em> (frames
 * without an {@code id}) are answered with HTTP 202 and an empty body, per spec.
 * Bare GET (used by full SSE-style MCP clients) returns 405 — this endpoint is JSON-RPC
 * over plain POST, not the bidirectional SSE transport. CORS preflight is handled by the
 * project-wide {@code CORSFilter} (mapped to {@code /mcp} in {@code web.xml}).
 */
public class MCPRequestHandler extends LogicsRequestHandler implements HttpRequestHandler {

    /**
     * Hard cap on inbound JSON-RPC body size — generous for tools/call arguments AND for
     * inline file inputs (xlsx/pdf templates AI hands the script for IMPORT / PRINT FROM),
     * but bounded so a hostile client cannot drive memory before per-tool caps engage.
     * 16 MiB raw ⇒ ~12 MiB binary after base64 decoding — fits typical document templates
     * with room for the rest of the JSON-RPC envelope.
     */
    public static final int MAX_BODY_BYTES = 16 * 1024 * 1024;

    public MCPRequestHandler(LogicsProvider logicsProvider) {
        super(logicsProvider);
    }

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) {
            response.setHeader("Allow", "POST, OPTIONS");
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Use POST with a JSON-RPC body");
            return;
        }
        if (!"POST".equalsIgnoreCase(method)) {
            response.setHeader("Allow", "POST, OPTIONS");
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        // MCP authorization spec: a protected-resource request without a valid token must
        // produce 401 + {@code WWW-Authenticate} pointing at the resource-metadata discovery
        // URL. claude.ai / Cursor / Claude Desktop key off this response to start the OAuth
        // dance — without it, they assume no auth is required and never run the flow.
        //
        // The "do we know who's calling?" decision (valid JWT signature; expiry; "Authorization:
        // Bearer anonymous" distinguished from missing header; missing-header challenged
        // unless enableAPI=2 explicitly allows anonymous) lives server-side in
        // {@link OAuthOperations#VALIDATE_TOKEN} so the web tier doesn't need to read server
        // settings or the JWT secret. The per-tool, per-user "is this caller allowed to do
        // this thing?" decision is pushed further downstream to action-level checks inside
        // {@link lsfusion.server.physics.admin.mcp.MCPDispatcher} (for tools that touch app
        // state — {@code lsfusion_files_*}, {@code lsfusion_eval}); doc-search / validate /
        // tools/list run for any caller who passed authentication. We pass
        // {@code header_present} so the dispatcher can tell {@code Authorization: Bearer
        // anonymous} (a malformed bearer — always reject) from a truly missing header (a
        // fresh client about to start OAuth — challenge unless enableAPI=2). The op returns
        // an RFC 6749 §5.2 error envelope when authentication fails; we map that to 401
        // with discovery challenge.
        AuthenticationToken token = LSFAuthenticationToken.getAppServerToken();
        boolean headerPresent = request.getHeader("Authorization") != null
                && !request.getHeader("Authorization").isEmpty();
        JSONObject validateRequest = new JSONObject().put("header_present", headerPresent);
        String validateResp = runRequest(request, (sessionObject, retry) ->
                sessionObject.remoteLogics.oauth(token, OAuthOperations.VALIDATE_TOKEN,
                        validateRequest.toString()));
        if (new JSONObject(validateResp).has("error")) {
            sendDiscoveryChallenge(request, response);
            return;
        }

        // Pre-check Content-Length so a client that advertises a huge body gets a clean 413.
        int declaredLength = request.getContentLength();
        if (declaredLength > MAX_BODY_BYTES) {
            response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                    "MCP body exceeds " + MAX_BODY_BYTES + " bytes");
            return;
        }

        final byte[] bodyBytes;
        try {
            bodyBytes = RequestUtils.readBoundedBody(request, MAX_BODY_BYTES);
        } catch (RequestUtils.BodyTooLargeException e) {
            response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                    "MCP body exceeds " + MAX_BODY_BYTES + " bytes");
            return;
        }
        final String body = new String(bodyBytes, StandardCharsets.UTF_8);

        ConnectionInfo connectionInfo = RequestUtils.getConnectionInfo(request);

        MCPResult result = runRequest(request, (sessionObject, retry) -> {
            ExternalRequest envelope = RequestUtils.buildExternalRequest(request, bodyBytes, sessionObject);
            return sessionObject.remoteLogics.mcp(token, connectionInfo, envelope, body);
        });

        if (result == null || result.json == null) {
            // JSON-RPC notification — acknowledge with 202 and no body.
            response.setStatus(HttpServletResponse.SC_ACCEPTED);
            return;
        }

        String json = resolveFiles(result, request);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json; charset=utf-8");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        response.setContentLength(bytes.length);
        try (OutputStream os = response.getOutputStream()) {
            os.write(bytes);
        }
    }

    /**
     * Walk every binary side-map entry through {@link FileUtils#saveMCPFile} (which sanitizes
     * the display name itself), then replace each placeholder string in {@code result.json}
     * with the path-absolute URL the download handler will serve.
     *
     * <p>Replacement walks the JSON tree (not raw string-replace) so a placeholder embedded in
     * a longer string doesn't get partially substituted, and so we touch only string-typed
     * leaves. After walking the tree, {@code result.content[0].text} — the JSON-stringified
     * mirror of {@code structuredContent} produced server-side — is regenerated from the
     * now-resolved structured payload; otherwise placeholders survive in {@code content[0].text}
     * even though {@code structuredContent.results[].url} got resolved, and clients reading
     * {@code content} (rather than {@code structuredContent}) see broken URLs.
     */
    private static String resolveFiles(MCPResult result, HttpServletRequest request) {
        if (result.files == null || result.files.isEmpty()) return result.json;

        String contextPath = request.getContextPath();
        String prefix = (contextPath == null ? "" : contextPath) + "/";

        Map<String, String> placeholderToUrl = new HashMap<>(result.files.size());
        for (Map.Entry<String, NamedFileData> e : result.files.entrySet()) {
            String url = FileUtils.saveMCPFile(e.getValue());
            placeholderToUrl.put(e.getKey(), prefix + url);
        }

        JSONObject root = new JSONObject(result.json);
        replacePlaceholders(root, placeholderToUrl);

        // Re-stringify structuredContent into content[0].text — the server-side mirror was
        // produced before placeholder resolution, so without this re-sync MCP clients reading
        // content[*] (instead of structuredContent) see __MCP_FILE_…__ literals.
        JSONObject rpcResult = root.optJSONObject("result");
        if (rpcResult != null) {
            JSONObject structured = rpcResult.optJSONObject("structuredContent");
            JSONArray content = rpcResult.optJSONArray("content");
            if (structured != null && content != null && content.length() > 0) {
                JSONObject first = content.optJSONObject(0);
                if (first != null && "text".equals(first.optString("type"))) {
                    first.put("text", structured.toString());
                }
            }
        }
        return root.toString();
    }

    private static void replacePlaceholders(Object node, Map<String, String> placeholderToUrl) {
        if (node instanceof JSONObject) {
            JSONObject o = (JSONObject) node;
            // Snapshot keys before mutating: JSONObject.keySet() is a live view and put() during
            // iteration would throw ConcurrentModificationException.
            List<String> keys = new ArrayList<>(o.keySet());
            for (String k : keys) {
                Object v = o.get(k);
                if (v instanceof String) {
                    String url = placeholderToUrl.get(v);
                    if (url != null) o.put(k, url);
                } else if (v instanceof JSONObject || v instanceof JSONArray) {
                    replacePlaceholders(v, placeholderToUrl);
                }
            }
        } else if (node instanceof JSONArray) {
            JSONArray a = (JSONArray) node;
            for (int i = 0; i < a.length(); i++) {
                Object v = a.get(i);
                if (v instanceof String) {
                    String url = placeholderToUrl.get(v);
                    if (url != null) a.put(i, url);
                } else if (v instanceof JSONObject || v instanceof JSONArray) {
                    replacePlaceholders(v, placeholderToUrl);
                }
            }
        }
    }

    /**
     * Emit the standard MCP discovery 401: {@code WWW-Authenticate} pointing at the
     * resource-metadata URL plus a {@code 401 Unauthorized} status. Both "no token" and
     * "invalid token" paths funnel through here so the response shape is identical, which
     * is what claude.ai / Cursor / Claude Desktop expect to drive the OAuth flow.
     *
     * <p>The body is a JSON-RPC 2.0 error envelope rather than a default servlet error
     * page — claude.ai's connector parses {@code /mcp} responses as JSON unconditionally
     * and surfaces the generic "Couldn't reach the MCP server" message when parsing fails,
     * which the Tomcat HTML error page would trigger. With a proper JSON body the OAuth
     * discovery handshake completes cleanly: claude.ai reads the {@code WWW-Authenticate}
     * header (which is what actually drives OAuth) without choking on the body.
     */
    private static void sendDiscoveryChallenge(HttpServletRequest request, HttpServletResponse response) throws IOException {
        OAuthRequestHandlerBase.setBearerChallengeHeader(request, response);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=utf-8");
        // -32001 is a server-defined JSON-RPC code; the HTTP status + WWW-Authenticate are
        // the canonical signal — body is informational/diagnostic only.
        response.getWriter().write(
                "{\"jsonrpc\":\"2.0\",\"id\":null,"
                        + "\"error\":{\"code\":-32001,\"message\":\"Authentication required\"}}");
    }

}
