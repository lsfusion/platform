package lsfusion.http.controller;

import lsfusion.http.authentication.LSFAuthenticationToken;
import lsfusion.http.provider.logics.LogicsProvider;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.connection.ConnectionInfo;
import lsfusion.interop.logics.LogicsSessionObject;
import lsfusion.interop.session.ExternalRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Plain-HTTP face of the classpath file tools: the non-MCP / non-JSON-RPC sibling of
 * {@code /mcp}'s {@code lsfusion_files_*}, symmetric with {@code /eval}. Returns the raw
 * {@code MCPFileTools} payload; the operation comes from the {@code /files/<op>} sub-path.
 * Authentication and per-role {@code enableAPI} gating are handled by the API security
 * chain and server-side {@code access()}.
 */
public class FilesRequestHandler extends ExternalRequestHandler {

    // These endpoints carry only tool args JSON, never file uploads.
    private static final int MAX_BODY_BYTES = 256 * 1024;

    // ExternalRequestHandler re-invokes handleRequest on a NoSuchObjectException retry (stale
    // pooled RMI stub — e.g. right after a BLB restart) with the SAME request, whose input stream
    // is already drained from the first attempt. Memoize the body on a request attribute so the
    // retry reuses it instead of forwarding an empty args body (which would silently fall back to
    // default args). MCPRequestHandler avoids this by reading the body before runRequest; here the
    // read lives inside the per-attempt method, so we cache per request instead.
    private static final String BODY_ATTRIBUTE = FilesRequestHandler.class.getName() + ".body";

    public FilesRequestHandler(LogicsProvider logicsProvider) {
        super(logicsProvider);
    }

    @Override
    protected void handleRequest(LogicsSessionObject sessionObject, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            response.setHeader("Allow", "POST");
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        String operation = getOperation(request);
        if (operation == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Unknown files operation");
            return;
        }

        final byte[] bodyBytes;
        byte[] cachedBody = (byte[]) request.getAttribute(BODY_ATTRIBUTE);
        if (cachedBody != null) {
            bodyBytes = cachedBody;
        } else {
            try {
                bodyBytes = RequestUtils.readBoundedBody(request, MAX_BODY_BYTES);
            } catch (RequestUtils.BodyTooLargeException e) {
                response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                        "Files body exceeds " + MAX_BODY_BYTES + " bytes");
                return;
            }
            request.setAttribute(BODY_ATTRIBUTE, bodyBytes);
        }

        AuthenticationToken token = LSFAuthenticationToken.getAppServerToken();
        ConnectionInfo connectionInfo = RequestUtils.getConnectionInfo(request);
        ExternalRequest envelope = RequestUtils.buildExternalRequest(request, bodyBytes, sessionObject);
        String json = sessionObject.remoteLogics.files(token, connectionInfo, envelope, operation, new String(bodyBytes, StandardCharsets.UTF_8));

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json; charset=utf-8");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        response.setContentLength(bytes.length);
        try (OutputStream os = response.getOutputStream()) {
            os.write(bytes);
        }
    }

    private static String getOperation(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || !pathInfo.startsWith("/") || pathInfo.length() == 1)
            return null;

        String operation = pathInfo.substring(1);
        return ("list".equals(operation) || "search".equals(operation) || "read".equals(operation)) ? operation : null;
    }
}
