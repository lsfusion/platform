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
 * <p><strong>Context-path caveat (RFC 8414 §3.1) — REQUIRED HTTP-layer rewrite for
 * non-root deploys, otherwise OAuth-discovery clients fail silently at the very first
 * step with a generic "couldn't reach the server" error and no useful server-side log.</strong>
 *
 * <p>For an issuer with a non-empty path (e.g. {@code https://host/lsfusion} deployed at
 * context path {@code /lsfusion}), RFC 8414 §3.1 mandates that the discovery URL be
 * {@code https://host/.well-known/oauth-authorization-server/lsfusion} — the well-known
 * segment lives at host root and the issuer path is appended <em>after</em> it. The Servlet
 * API can't claim that URL from inside a non-root context: a webapp deployed at
 * {@code /lsfusion} only owns {@code /lsfusion/*}. So the natural URL we serve here is
 * {@code https://host/lsfusion/.well-known/oauth-authorization-server} — append-style.
 *
 * <p>Some clients fall back from the strict path to the append path on 404, others don't.
 * Verified against claude.ai's connector backend (2026-05): no fallback — the client
 * abandons discovery after the strict path 404s and the user sees a generic error.
 * Treat this as the general case: any RFC-8414-strict OAuth or OIDC client may behave
 * the same way, and fallback behavior is implementation-specific and undocumented.
 *
 * <p>Fix at the HTTP layer in front of the webapp, not in application code. Whatever
 * component is in front of the Java app — Tomcat itself, nginx / Apache reverse proxy,
 * Traefik, k8s ingress controller, CDN — needs a rewrite that captures the issuer-path
 * segment and re-routes the request into the right context. Same idea everywhere; for
 * a Tomcat host-level {@code RewriteValve} the rules are:
 * <pre>
 *   RewriteRule ^/\.well-known/oauth-authorization-server/(.+)$ /$1/.well-known/oauth-authorization-server [L]
 *   RewriteRule ^/\.well-known/openid-configuration/(.+)$        /$1/.well-known/oauth-authorization-server [L]
 * </pre>
 * Generic — works for {@code /mm}, {@code /mycompany-ru}, any new context, no per-context
 * config. The OIDC line aliases OIDC discovery onto the OAuth metadata document; we don't
 * claim to be a full OIDC provider, and the alias closes the OIDC-discovery 404 chain that
 * some clients walk after the OAuth one. Root-context deploys (issuer = {@code https://host})
 * don't hit any of this because the strict and append URLs collapse to the same
 * {@code /.well-known/oauth-authorization-server}.
 *
 * <p>See {@code platform/docs/oauth-authorization-server-issue.md} (deployment section)
 * for the full diagnostic trail, equivalents for nginx / Apache / Traefik / k8s ingress,
 * and verification commands.
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
