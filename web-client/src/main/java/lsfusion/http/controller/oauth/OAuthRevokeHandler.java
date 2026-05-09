package lsfusion.http.controller.oauth;

import lsfusion.http.provider.logics.LogicsProvider;
import lsfusion.interop.oauth.OAuthOperations;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * RFC 7009 token revocation endpoint — form-encoded body with {@code token} (and optional
 * {@code token_type_hint}). Per spec, the response is HTTP 200 with empty body on both
 * "successfully revoked" and "no such token", so the caller can't probe for token validity.
 *
 * <p>MVP scope mirrors the dispatcher: only refresh tokens are tracked, so
 * {@code token_type_hint=refresh_token} is the only meaningful hint and an opaque access
 * JWT here is silently no-op'd (still returns 200, which matches §2.2 behavior for unknown
 * tokens). Public clients without a {@code client_secret} authenticate themselves by PKCE
 * at the original token grant; revocation of a public client's token is unauthenticated by
 * design (the holder of the token revokes it).
 */
public class OAuthRevokeHandler extends OAuthRequestHandlerBase {

    public OAuthRevokeHandler(LogicsProvider logicsProvider) {
        super(logicsProvider);
    }

    @Override
    protected void handleOAuth(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String token = request.getParameter("token");
        if (token == null || token.isEmpty()) {
            writeJson(response,
                    new JSONObject().put("error", "invalid_request")
                            .put("error_description", "token is required"),
                    HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        JSONObject requestJson = new JSONObject().put("token", token);
        String hint = request.getParameter("token_type_hint");
        if (hint != null && !hint.isEmpty()) requestJson.put("token_type_hint", hint);

        invokeOAuth(request, response, OAuthOperations.REVOKE_TOKEN, requestJson,
                HttpServletResponse.SC_OK);
    }
}
