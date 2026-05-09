package lsfusion.http.controller.oauth;

import lsfusion.http.controller.RequestUtils;
import lsfusion.http.provider.logics.LogicsProvider;
import lsfusion.interop.oauth.OAuthOperations;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * RFC 7591 §3 dynamic client registration — accepts a JSON body describing the client
 * and forwards it to {@link OAuthOperations#REGISTER_CLIENT}. The request shape is
 * the standard OAuth one (used by claude.ai, Cursor, Claude Desktop):
 *
 * <pre>{@code
 * { "client_name": "Claude Desktop",
 *   "redirect_uris": ["https://claude.ai/api/.../oauth/callback"],
 *   "token_endpoint_auth_method": "none" }
 * }</pre>
 *
 * On success returns {@code 201 Created} with the registered metadata + server-issued
 * {@code client_id}; on validation failure, the dispatcher's
 * {@code invalid_client_metadata} / {@code invalid_redirect_uri} error envelope is
 * surfaced as 400.
 */
public class OAuthRegisterHandler extends OAuthRequestHandlerBase {

    /** Hard cap on registration body size — much smaller than {@code /mcp} since this
     *  is purely metadata, no inline binaries. */
    private static final int MAX_BODY_BYTES = 64 * 1024;

    public OAuthRegisterHandler(LogicsProvider logicsProvider) {
        super(logicsProvider);
    }

    @Override
    protected void handleOAuth(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // Pre-check Content-Length header so a client that advertises an oversized body gets
        // a clean 413 without the server having to read past the cap.
        int declaredLength = request.getContentLength();
        if (declaredLength > MAX_BODY_BYTES) {
            response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                    "Request body exceeds " + MAX_BODY_BYTES + " bytes");
            return;
        }
        String body;
        try {
            body = new String(RequestUtils.readBoundedBody(request, MAX_BODY_BYTES), StandardCharsets.UTF_8);
        } catch (RequestUtils.BodyTooLargeException e) {
            response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                    "Request body exceeds " + MAX_BODY_BYTES + " bytes");
            return;
        }
        JSONObject requestJson;
        try {
            requestJson = body.isEmpty() ? new JSONObject() : new JSONObject(body);
        } catch (Exception e) {
            writeJson(response,
                    new JSONObject().put("error", "invalid_client_metadata")
                            .put("error_description", "Request body must be a JSON object: " + e.getMessage()),
                    HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        // Per RFC 7591 §3.2.1 success status is 201.
        invokeOAuth(request, response, OAuthOperations.REGISTER_CLIENT, requestJson,
                HttpServletResponse.SC_CREATED);
    }

}
