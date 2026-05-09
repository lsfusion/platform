package lsfusion.http.controller.oauth;

import lsfusion.http.provider.logics.LogicsProvider;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Serves {@code GET /.well-known/oauth-authorization-server} per RFC 8414 — the document
 * MCP clients (claude.ai, Claude Desktop, Cursor) fetch right after a 401 from {@code /mcp}
 * to discover where to register and run the OAuth flow.
 *
 * <p>The metadata is built from the request's external base URL on every call (no static
 * config), so the same web-client deployment behind nginx-on-different-host works without
 * a settings change. The fields included are exactly the minimum claude.ai's strict OAuth
 * client validates against; we don't omit any of them even when their values are obvious,
 * since some clients reject documents missing required keys.
 *
 * <p><strong>Context-path caveat (RFC 8414 §3.1):</strong> for an issuer with a non-empty
 * path (e.g. {@code https://host/lsfusion} when deployed at context path {@code /lsfusion}),
 * the strictly-spec-compliant well-known URL is
 * {@code https://host/.well-known/oauth-authorization-server/lsfusion} — i.e. the path
 * goes <em>after</em> the well-known segment, not the webapp-natural
 * {@code https://host/lsfusion/.well-known/oauth-authorization-server} we serve here.
 * Most real OAuth clients accept both forms, but for strict spec compliance the recommended
 * deployment is a root context path. If your deployment uses a non-root context, a reverse
 * proxy alias from the spec-shaped URL to ours is the fix.
 */
public class WellKnownAuthorizationServerHandler extends OAuthRequestHandlerBase {

    public WellKnownAuthorizationServerHandler(LogicsProvider logicsProvider) {
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
                .put("issuer", base)
                .put("authorization_endpoint", base + "/oauth/authorize")
                .put("token_endpoint", base + "/oauth/token")
                .put("registration_endpoint", base + "/oauth/register")
                .put("revocation_endpoint", base + "/oauth/revoke")
                .put("response_types_supported", new JSONArray().put("code"))
                .put("grant_types_supported",
                        new JSONArray().put("authorization_code").put("refresh_token"))
                .put("code_challenge_methods_supported", new JSONArray().put("S256"))
                .put("token_endpoint_auth_methods_supported", new JSONArray().put("none"))
                .put("revocation_endpoint_auth_methods_supported", new JSONArray().put("none"));
        writeJson(response, metadata, HttpServletResponse.SC_OK);
    }
}
