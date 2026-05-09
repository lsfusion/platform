package lsfusion.http.controller.oauth;

import lsfusion.http.provider.logics.LogicsProvider;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Serves {@code GET /.well-known/oauth-protected-resource} per RFC 9728 — the first
 * discovery hop a client makes after seeing the 401 + {@code WWW-Authenticate} from
 * {@code /mcp}. Tells the client which authorization server to use.
 *
 * <p>The {@code resource} field points at our {@code /mcp} endpoint specifically. claude.ai's
 * connector validator compares this against the URL it got 401 on and aborts the flow if the
 * advertised resource doesn't match. Other API endpoints (e.g. {@code /eval}) reuse the same
 * AS via the same {@code WWW-Authenticate} header — the issued Bearer is accepted at any
 * endpoint of this resource server — but they don't get their own protected-resource doc.
 * The {@code authorization_servers} array lists the AS issuer (same host, since lsFusion
 * plays both roles in this deployment); one additional hop from here lands the client at
 * {@link WellKnownAuthorizationServerHandler}.
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
                .put("resource", base + "/mcp")
                .put("authorization_servers", new JSONArray().put(base))
                .put("bearer_methods_supported", new JSONArray().put("header"));
        writeJson(response, metadata, HttpServletResponse.SC_OK);
    }
}
