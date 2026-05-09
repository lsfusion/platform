package lsfusion.http.controller.oauth;

import lsfusion.http.authentication.LSFAuthenticationToken;
import lsfusion.http.controller.LogicsRequestHandler;
import lsfusion.http.provider.logics.LogicsProvider;
import lsfusion.interop.connection.AuthenticationToken;
import org.json.JSONObject;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Common plumbing for {@code /oauth/*} HTTP endpoints — they all forward to
 * {@link lsfusion.interop.logics.remote.RemoteLogicsInterface#oauth} with a
 * different {@code operation} string and parse JSON in / out, so the parsing,
 * dispatch, error mapping, and response shaping live here.
 *
 * <p>Per RFC 6749 §5.2 the dispatcher returns either a success-shaped JSON object or
 * an error envelope ({@code {"error": ..., "error_description": ...}}); this class
 * maps each known error code to the correct HTTP status (400 / 401 / 500) so the
 * concrete handlers don't need to repeat the table.
 *
 * <p>Subclasses just override
 * {@link #handleOAuth(HttpServletRequest, HttpServletResponse)} (which receives a
 * pre-validated request) and call {@link #invokeOAuth} with the operation name and
 * a request-JSON they assemble from query/form/body params.
 */
public abstract class OAuthRequestHandlerBase extends LogicsRequestHandler implements HttpRequestHandler {

    /** Default HTTP status for an unmapped OAuth error code. RFC 6749 §5.2 specifies 400. */
    private static final int DEFAULT_ERROR_STATUS = HttpServletResponse.SC_BAD_REQUEST;

    public OAuthRequestHandlerBase(LogicsProvider logicsProvider) {
        super(logicsProvider);
    }

    @Override
    public final void handleRequest(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // Per RFC 6749 §3.2 token requests must be POST; same convention applies to register/
        // revoke. Discovery (.well-known/*) overrides this method to allow GET.
        String method = request.getMethod();
        if (!"POST".equalsIgnoreCase(method) && !allowsMethod(method)) {
            response.setHeader("Allow", allowedMethods());
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        handleOAuth(request, response);
    }

    /** Override to allow extra HTTP methods (e.g. GET on discovery endpoints). */
    protected boolean allowsMethod(String method) {
        return false;
    }

    /** {@code Allow} response header for 405. */
    protected String allowedMethods() {
        return "POST, OPTIONS";
    }

    /**
     * Subclass-implemented operation. Receives a pre-validated request (HTTP method already
     * checked) and is expected to either write a final response or call
     * {@link #invokeOAuth} which handles success/error mapping uniformly.
     */
    protected abstract void handleOAuth(HttpServletRequest request, HttpServletResponse response)
            throws IOException;

    /**
     * Dispatch one OAuth operation through {@code RemoteLogicsInterface.oauth}, handling the
     * full success/error flow:
     *
     * <ul>
     *   <li>Picks up the caller's auth context via {@link LSFAuthenticationToken#getAppServerToken}.
     *   <li>On success, writes the dispatcher's JSON response with the given HTTP status.
     *   <li>On RFC 6749 §5.2 error envelope ({@code "error"} key in response), maps the code
     *     to the appropriate HTTP status and writes the envelope as the response body.
     * </ul>
     */
    protected final void invokeOAuth(HttpServletRequest request, HttpServletResponse response,
                                     String operation, JSONObject requestJson, int successStatus)
            throws IOException {
        AuthenticationToken token = LSFAuthenticationToken.getAppServerToken();
        String responseJson = runRequest(request, (sessionObject, retry) ->
                sessionObject.remoteLogics.oauth(token, operation, requestJson.toString()));
        writeJsonOrError(response, responseJson, successStatus);
    }

    /**
     * Inspect the dispatcher's JSON response: if it looks like an error envelope ({@code error}
     * field present), map to HTTP status; otherwise write as success.
     */
    protected void writeJsonOrError(HttpServletResponse response, String responseJson, int successStatus)
            throws IOException {
        JSONObject parsed = new JSONObject(responseJson);
        if (parsed.has("error")) {
            int status = errorCodeToStatus(parsed.optString("error"));
            writeJson(response, parsed, status);
        } else {
            writeJson(response, parsed, successStatus);
        }
    }

    protected static void writeJson(HttpServletResponse response, JSONObject body, int status)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json; charset=utf-8");
        // OAuth responses must not be cached (RFC 6749 §5.1 requires Cache-Control: no-store
        // and Pragma: no-cache on token responses; harmless on the others).
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        response.setContentLength(bytes.length);
        try (OutputStream os = response.getOutputStream()) {
            os.write(bytes);
        }
    }

    /**
     * RFC 6749 §5.2 → HTTP status. {@code invalid_client} is the only one that gets 401
     * (per spec, also forces clients to re-authenticate); everything else is 400 except
     * {@code server_error} which is 500.
     */
    protected static int errorCodeToStatus(String error) {
        if (error == null) return DEFAULT_ERROR_STATUS;
        switch (error) {
            case "invalid_client":      return HttpServletResponse.SC_UNAUTHORIZED;
            case "server_error":        return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            case "temporarily_unavailable": return HttpServletResponse.SC_SERVICE_UNAVAILABLE;
            default:                    return DEFAULT_ERROR_STATUS;
        }
    }

    /**
     * Build the externally-visible base URL ({@code scheme://host[:port][/contextPath}}) from
     * the inbound request. Used to interpolate absolute URIs into discovery JSON. Honors
     * {@code X-Forwarded-Proto} / {@code X-Forwarded-Host} for deployments behind a TLS-
     * terminating proxy where {@code request.getScheme()} reports {@code http}.
     *
     * <p><strong>Trust assumption:</strong> the forwarded headers are taken at face value.
     * Production deployments MUST ensure the app server is reachable only via the trusted
     * proxy (firewall / bind to internal interface), since a client able to talk to the app
     * server directly could spoof these headers and influence the issuer/endpoint URLs the
     * server publishes. A proper allowlist or settings-based override is a future hardening
     * step (would resolve the same trust issue for {@link MCPRequestHandler}'s
     * {@code WWW-Authenticate} header).
     */
    public static String externalBaseUrl(HttpServletRequest request) {
        String scheme = firstNonEmpty(request.getHeader("X-Forwarded-Proto"), request.getScheme());
        String host = firstNonEmpty(request.getHeader("X-Forwarded-Host"), request.getServerName());
        // Port is part of host header when forwarded; otherwise build from scheme/serverPort.
        StringBuilder url = new StringBuilder(scheme).append("://").append(host);
        if (host.indexOf(':') < 0) { // no port in host header
            int port = request.getServerPort();
            boolean defaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                    || ("https".equalsIgnoreCase(scheme) && port == 443);
            if (!defaultPort) url.append(':').append(port);
        }
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty()) url.append(ctx);
        return url.toString();
    }

    private static String firstNonEmpty(String first, String fallback) {
        return first != null && !first.isEmpty() ? first : fallback;
    }

    /**
     * Set the OAuth-discovery {@code WWW-Authenticate: Bearer ...} header pointing at the
     * shared {@code /.well-known/oauth-protected-resource} document. Any protected lsFusion
     * API endpoint that wants MCP-style auto-discovery (claude.ai / Cursor / Claude Desktop
     * walking the chain on 401) calls this before emitting its 401. The body and exact
     * status-line message stay with the caller — only the header is shared.
     */
    public static void setBearerChallengeHeader(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("WWW-Authenticate",
                "Bearer realm=\"lsfusion\", "
                        + "resource_metadata=\"" + externalBaseUrl(request) + "/.well-known/oauth-protected-resource\"");
    }
}
