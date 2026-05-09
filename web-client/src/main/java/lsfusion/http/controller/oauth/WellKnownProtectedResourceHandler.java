package lsfusion.http.controller.oauth;

import lsfusion.http.provider.logics.LogicsProvider;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Serves {@code GET /.well-known/oauth-protected-resource} per RFC 9728 — the first
 * discovery hop a client makes after seeing the 401 + {@code WWW-Authenticate} from any
 * protected lsFusion API endpoint ({@code /mcp}, {@code /eval}, {@code /exec}, …). Tells
 * the client which authorization server to use.
 *
 * <p>The {@code resource} URL is the base URL of the resource server itself rather than
 * any single endpoint, since one Bearer issued by our AS is accepted by every API endpoint
 * under that base. The {@code authorization_servers} array lists the AS issuer (same host,
 * since lsFusion plays both roles in this deployment). One additional hop from here lands
 * the client at {@link WellKnownAuthorizationServerHandler}.
 */
public class WellKnownProtectedResourceHandler extends OAuthRequestHandlerBase {

    public WellKnownProtectedResourceHandler(LogicsProvider logicsProvider) {
        super(logicsProvider);
    }

    @Override
    protected boolean allowsMethod(String method) {
        return "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method);
    }

    @Override
    protected String allowedMethods() {
        return "GET, HEAD, OPTIONS";
    }

    @Override
    protected void handleOAuth(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String base = externalBaseUrl(request);
        JSONObject metadata = new JSONObject()
                .put("resource", base)
                .put("authorization_servers", new JSONArray().put(base))
                .put("bearer_methods_supported", new JSONArray().put("header"));
        writeJson(response, metadata, HttpServletResponse.SC_OK);
    }
}
