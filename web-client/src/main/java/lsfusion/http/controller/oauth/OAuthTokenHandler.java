package lsfusion.http.controller.oauth;

import lsfusion.http.provider.logics.LogicsProvider;
import lsfusion.interop.oauth.OAuthOperations;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * RFC 6749 §3.2 token endpoint — accepts {@code application/x-www-form-urlencoded} bodies
 * (per spec, NOT JSON) for two grant types:
 *
 * <ul>
 *   <li>{@code grant_type=authorization_code}: exchange auth_code + PKCE verifier for access
 *     JWT + refresh token. Maps to {@link OAuthOperations#EXCHANGE_CODE}.
 *   <li>{@code grant_type=refresh_token}: exchange a refresh token for a fresh access JWT.
 *     Maps to {@link OAuthOperations#REFRESH_TOKEN}.
 * </ul>
 *
 * <p>Servlet-container parsing handles the URL-decoding of form params for us
 * (via {@link HttpServletRequest#getParameter}). Unknown {@code grant_type} returns
 * {@code unsupported_grant_type} per RFC 6749 §5.2.
 */
public class OAuthTokenHandler extends OAuthRequestHandlerBase {

    public OAuthTokenHandler(LogicsProvider logicsProvider) {
        super(logicsProvider);
    }

    @Override
    protected void handleOAuth(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String grantType = request.getParameter("grant_type");
        if (grantType == null || grantType.isEmpty()) {
            writeJson(response,
                    new JSONObject().put("error", "invalid_request")
                            .put("error_description", "grant_type is required"),
                    HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        JSONObject requestJson = new JSONObject();
        String operation;
        switch (grantType) {
            case "authorization_code":
                operation = OAuthOperations.EXCHANGE_CODE;
                copyParam(request, requestJson, "code");
                copyParam(request, requestJson, "code_verifier");
                copyParam(request, requestJson, "client_id");
                copyParam(request, requestJson, "redirect_uri");
                break;
            case "refresh_token":
                operation = OAuthOperations.REFRESH_TOKEN;
                copyParam(request, requestJson, "refresh_token");
                copyParam(request, requestJson, "client_id");
                break;
            default:
                writeJson(response,
                        new JSONObject().put("error", "unsupported_grant_type")
                                .put("error_description", "Unsupported grant_type: " + grantType),
                        HttpServletResponse.SC_BAD_REQUEST);
                return;
        }
        invokeOAuth(request, response, operation, requestJson, HttpServletResponse.SC_OK);
    }

    /** Copy {@code param} from form params into {@code dst} if present (skip null/empty). */
    private static void copyParam(HttpServletRequest request, JSONObject dst, String param) {
        String v = request.getParameter(param);
        if (v != null && !v.isEmpty()) dst.put(param, v);
    }
}
