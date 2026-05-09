package lsfusion.http.controller.oauth;

import lsfusion.http.authentication.LSFAuthenticationToken;
import lsfusion.http.authentication.LSFLoginUrlAuthenticationEntryPoint;
import lsfusion.http.provider.logics.LogicsProvider;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.oauth.OAuthOperations;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * RFC 6749 §4.1.1 authorization endpoint — the browser-flow part of the OAuth Authorization
 * Server. Sequence:
 *
 * <ol>
 *   <li>Validate {@code client_id} + {@code redirect_uri} against the registered clients
 *       (via dispatcher's {@link OAuthOperations#GET_CLIENT}). Failures here return a direct
 *       error response — we don't redirect to an unverified URI.</li>
 *   <li>Validate the rest of the OAuth params ({@code response_type=code}, PKCE
 *       {@code code_challenge} present, {@code code_challenge_method=S256}). Failures from
 *       this point on redirect to the verified {@code redirect_uri} with an error param,
 *       per RFC 6749 §4.1.2.1.</li>
 *   <li>Authenticate the user. Anonymous request ⇒ save the URL via
 *       {@link LSFLoginUrlAuthenticationEntryPoint#requestCache} and redirect to
 *       {@code /login}; the existing {@link
 *       lsfusion.http.authentication.LSFAuthenticationSuccessHandler} restores the URL
 *       after a successful login, landing the user back here authenticated.</li>
 *   <li>Show consent screen (skipped for clients flagged {@code trusted=true}). The form
 *       submits back to the same URL with {@code consent=approve} | {@code consent=deny}.</li>
 *   <li>On approve: call {@link OAuthOperations#ISSUE_AUTH_CODE} to mint a code bound to
 *       the user × client × redirect_uri × PKCE challenge, then redirect to
 *       {@code redirect_uri?code=...&state=...}.</li>
 *   <li>On deny: redirect with {@code error=access_denied} per RFC 6749 §4.1.2.1.</li>
 * </ol>
 *
 * <p>Spring Security wiring: {@code /oauth/authorize} is declared with
 * {@code permitAll()} inside the UI {@code <http>} block (NOT {@code security="none"}) —
 * the difference matters because the UI chain processes session cookies, so a user already
 * logged in via another tab is recognized here without re-login.
 */
public class OAuthAuthorizeHandler extends OAuthRequestHandlerBase {

    /** Session attribute key holding the per-session, per-client one-shot consent nonce. */
    private static final String CONSENT_NONCE_ATTR = "lsfusion.oauth.consent_nonce";
    /** Form field name for the consent CSRF nonce. */
    private static final String CONSENT_NONCE_PARAM = "consent_nonce";
    private static final SecureRandom RNG = new SecureRandom();

    public OAuthAuthorizeHandler(LogicsProvider logicsProvider) {
        super(logicsProvider);
    }

    @Override
    protected boolean allowsMethod(String method) {
        return "GET".equalsIgnoreCase(method) || "POST".equalsIgnoreCase(method);
    }

    @Override
    protected String allowedMethods() {
        return "GET, POST, OPTIONS";
    }

    @Override
    protected void handleOAuth(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String clientId = request.getParameter("client_id");
        String redirectURI = request.getParameter("redirect_uri");
        String responseType = request.getParameter("response_type");
        String codeChallenge = request.getParameter("code_challenge");
        String codeChallengeMethod = request.getParameter("code_challenge_method");
        String state = request.getParameter("state");
        // Consent / nonce ONLY honored on POST. A GET with `consent=approve` is the classic
        // CSRF vector for OAuth: an attacker could craft a top-level navigation URL the
        // logged-in victim follows (image src, link), and the cookie would silently authorize
        // the attacker's client. CSRF is disabled at the chain level for the rest of /oauth/*,
        // so we enforce it locally with a session-bound nonce on POST below.
        boolean isPost = "POST".equalsIgnoreCase(request.getMethod());
        String consent = isPost ? request.getParameter("consent") : null;
        String consentNonce = isPost ? request.getParameter(CONSENT_NONCE_PARAM) : null;

        // Step 1: required params + client lookup. Errors before redirect_uri is verified
        // become direct responses — we will not bounce a request to a URI we haven't
        // confirmed belongs to a registered client.
        if (clientId == null || clientId.isEmpty()) {
            sendDirectError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "invalid_request", "client_id is required");
            return;
        }
        if (redirectURI == null || redirectURI.isEmpty()) {
            sendDirectError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "invalid_request", "redirect_uri is required");
            return;
        }
        // Reject malformed / non-absolute URIs and URIs with a fragment up front. Fragments
        // would break success/error redirects (we add ?code=… via query, but
        // `https://x/cb#frag?code=…` puts code in the fragment-side, which the callback
        // can't read). Per RFC 6749 §3.1.2, redirect_uri MUST be absolute (scheme +
        // authority), and MUST NOT include a fragment component. Same check the registration
        // endpoint applies — duplicated here as defense-in-depth in case a record reached
        // the DB through some other path (e.g. admin form).
        URI parsedURI;
        try {
            parsedURI = new URI(redirectURI);
        } catch (URISyntaxException e) {
            sendDirectError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "invalid_request", "redirect_uri is not a valid URI");
            return;
        }
        if (parsedURI.getFragment() != null) {
            sendDirectError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "invalid_request", "redirect_uri must not contain a fragment");
            return;
        }
        String redirectScheme = parsedURI.getScheme();
        if (redirectScheme == null
                || (!"http".equalsIgnoreCase(redirectScheme) && !"https".equalsIgnoreCase(redirectScheme))
                || parsedURI.getRawAuthority() == null || parsedURI.getRawAuthority().isEmpty()) {
            sendDirectError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "invalid_request", "redirect_uri must be an absolute http(s) URI with an authority");
            return;
        }
        JSONObject clientMeta = lookupClient(request, clientId);
        if (clientMeta == null) {
            sendDirectError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "invalid_client", "Unknown client_id");
            return;
        }
        if (!isRedirectURIAllowed(clientMeta, redirectURI)) {
            sendDirectError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "invalid_request", "redirect_uri is not registered for this client");
            return;
        }

        // Step 2: remaining param checks. From here on, errors redirect back to the
        // (verified) redirect_uri with error params, per RFC 6749 §4.1.2.1.
        if (!"code".equals(responseType)) {
            sendOAuthErrorRedirect(response, redirectURI, "unsupported_response_type",
                    "Only response_type=code is supported", state);
            return;
        }
        if (codeChallenge == null || codeChallenge.isEmpty()) {
            sendOAuthErrorRedirect(response, redirectURI, "invalid_request",
                    "PKCE code_challenge is required", state);
            return;
        }
        if (codeChallengeMethod != null && !codeChallengeMethod.isEmpty()
                && !"S256".equals(codeChallengeMethod)) {
            sendOAuthErrorRedirect(response, redirectURI, "invalid_request",
                    "Only code_challenge_method=S256 is supported", state);
            return;
        }

        // Step 3: authenticate the user via session cookie. Anonymous ⇒ save the request
        // and redirect to /login; LSFAuthenticationSuccessHandler will land us back here.
        AuthenticationToken token = LSFAuthenticationToken.getAppServerToken();
        if (token.isAnonymous()) {
            LSFLoginUrlAuthenticationEntryPoint.requestCache.saveRequest(request);
            String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
            response.sendRedirect(contextPath + "/login");
            return;
        }

        // Step 4: consent. Trusted clients skip the dialog and the consent/nonce checks
        // entirely; the trusted flag in OAuthClient (admin-set) is the explicit declaration
        // that no consent UI is needed. Everyone else must POST {@code consent=approve} (or
        // {@code consent=deny}) with a matching CSRF nonce.
        boolean trusted = clientMeta.optBoolean("trusted", false);
        if (!trusted) {
            if (consent == null) {
                renderConsentForm(request, response, clientMeta);
                return;
            }
            // Non-null consent reaching here means we're inside a POST (isPost guard
            // upstream). Verify the session-bound nonce before honoring the value.
            if (!consumeConsentNonce(request, consentNonce)) {
                sendDirectError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "invalid_request", "Missing or invalid consent_nonce");
                return;
            }
            if ("deny".equals(consent)) {
                sendOAuthErrorRedirect(response, redirectURI, "access_denied",
                        "User denied the authorization request", state);
                return;
            }
            // Strict allow-list: only the literal "approve" reaches code issuance. Anything
            // else (typo, garbage, future-form value we don't know about) is a protocol
            // error rather than silent approval — the consent decision must be explicit.
            if (!"approve".equals(consent)) {
                sendDirectError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "invalid_request", "Unknown consent value: " + consent);
                return;
            }
        }

        // Step 5: issue the code via the dispatcher (which mints OAuthAuthCode bound to
        // user × client × redirect_uri × code_challenge with the configured TTL).
        JSONObject issueRequest = new JSONObject()
                .put("client_id", clientId)
                .put("redirect_uri", redirectURI)
                .put("code_challenge", codeChallenge)
                .put("code_challenge_method", "S256");
        String issueRespJson = runRequest(request, (sessionObject, retry) ->
                sessionObject.remoteLogics.oauth(token, OAuthOperations.ISSUE_AUTH_CODE,
                        issueRequest.toString()));
        JSONObject issueResp = new JSONObject(issueRespJson);
        if (issueResp.has("error")) {
            sendOAuthErrorRedirect(response, redirectURI, issueResp.optString("error"),
                    issueResp.optString("error_description"), state);
            return;
        }
        String code = issueResp.getString("code");

        // Success redirect: build query params structurally on top of any existing
        // redirect_uri query string. Fragment was already rejected above, so we can safely
        // append. State is echoed verbatim per RFC 6749 §4.1.2 — the client uses it to
        // correlate this redirect with the original /authorize request and detect CSRF.
        response.sendRedirect(appendQueryParams(parsedURI,
                "code", code,
                "state", state));
    }

    /**
     * Look up the client's safe-to-display metadata via the dispatcher's
     * {@link OAuthOperations#GET_CLIENT}. Returns {@code null} when unknown — the dispatcher
     * surfaces that as an {@code invalid_client} envelope which we map to "no such client"
     * here (the response-mapping difference only matters in tests; in production both shapes
     * mean the same thing to the caller).
     */
    private JSONObject lookupClient(HttpServletRequest request, String clientId) throws IOException {
        // Use anonymous token: GET_CLIENT op doesn't need authentication.
        AuthenticationToken anon = AuthenticationToken.ANONYMOUS;
        JSONObject reqJson = new JSONObject().put("client_id", clientId);
        String respStr = runRequest(request, (sessionObject, retry) ->
                sessionObject.remoteLogics.oauth(anon, OAuthOperations.GET_CLIENT, reqJson.toString()));
        JSONObject resp = new JSONObject(respStr);
        return resp.has("error") ? null : resp;
    }

    /**
     * Strict string-match the inbound {@code redirect_uri} against the array of registered
     * URIs in the client metadata, per RFC 6749 §3.1.2.3 (no normalization, no wildcards).
     */
    private static boolean isRedirectURIAllowed(JSONObject clientMeta, String redirectURI) {
        JSONArray allowed = clientMeta.optJSONArray("redirect_uris");
        if (allowed == null) return false;
        for (int i = 0; i < allowed.length(); i++) {
            if (redirectURI.equals(allowed.optString(i))) return true;
        }
        return false;
    }

    /**
     * Direct JSON error response — used only before the redirect_uri is verified. After
     * verification, errors flow through {@link #sendOAuthErrorRedirect}.
     */
    private static void sendDirectError(HttpServletResponse response, int status, String error, String desc)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json; charset=utf-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        JSONObject body = new JSONObject().put("error", error).put("error_description", desc);
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        response.setContentLength(bytes.length);
        try (OutputStream os = response.getOutputStream()) {
            os.write(bytes);
        }
    }

    /**
     * RFC 6749 §4.1.2.1 error redirect — bounce to {@code redirect_uri} with
     * {@code error}/{@code error_description}/{@code state} query params. Used once the
     * redirect_uri has been confirmed registered for this client. Parsed structurally to
     * survive an existing query string in the redirect_uri without producing
     * {@code https://x/cb?old=1?error=…}.
     */
    private static void sendOAuthErrorRedirect(HttpServletResponse response, String redirectURI,
                                               String error, String desc, String state) throws IOException {
        URI parsed;
        try {
            parsed = new URI(redirectURI);
        } catch (URISyntaxException e) {
            // Should not reach here — caller validated redirect_uri above; degrade to direct
            // 400 if something pathological happens.
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "redirect_uri became invalid");
            return;
        }
        response.sendRedirect(appendQueryParams(parsed,
                "error", error,
                "error_description", desc,
                "state", state));
    }

    /**
     * Append {@code (name, value, name, value, …)} pairs to the URI's query string,
     * preserving any pre-existing query and authority. Empty/null values skip their
     * pair entirely. Returns the assembled URI as a string suitable for {@code sendRedirect}.
     *
     * <p>Built manually with raw components rather than the multi-arg {@link URI}
     * constructor: the latter re-percent-encodes its inputs, which would double-encode our
     * already-{@code urlEncode}-d values (turning {@code %3D} into {@code %253D} in the
     * final URL). Concatenating raw parts is the spec-correct way when the query is already
     * encoded.
     */
    private static String appendQueryParams(URI uri, String... nameValuePairs) {
        StringBuilder query = new StringBuilder();
        if (uri.getRawQuery() != null) {
            query.append(uri.getRawQuery());
        }
        for (int i = 0; i + 1 < nameValuePairs.length; i += 2) {
            String name = nameValuePairs[i];
            String value = nameValuePairs[i + 1];
            if (value == null || value.isEmpty()) continue;
            if (query.length() > 0) query.append('&');
            query.append(name).append('=').append(urlEncode(value));
        }
        StringBuilder result = new StringBuilder();
        result.append(uri.getScheme()).append("://").append(uri.getRawAuthority());
        if (uri.getRawPath() != null) result.append(uri.getRawPath());
        if (query.length() > 0) result.append('?').append(query);
        // Fragment intentionally skipped — fragments are rejected upstream, and OAuth
        // redirects must not include one (RFC 6749 §3.1.2).
        return result.toString();
    }

    /**
     * Render the consent form. Tiny self-contained HTML — no JSP dependency, since this
     * handler is mounted on a {@link org.springframework.web.context.support.HttpRequestHandlerServlet}
     * not a Spring MVC view resolver. Hidden fields preserve the OAuth params across the
     * POST so the second pass through {@link #handleOAuth} sees the same context.
     */
    private static void renderConsentForm(HttpServletRequest request, HttpServletResponse response,
                                          JSONObject clientMeta) throws IOException {
        String clientName = clientMeta.optString("client_name", "an application");
        String nonce = mintConsentNonce(request);
        StringBuilder html = new StringBuilder(2048);
        html.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\">");
        html.append("<title>Authorize ").append(htmlEscape(clientName)).append("</title>");
        html.append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">");
        html.append("<style>");
        html.append("body{font-family:-apple-system,BlinkMacSystemFont,Segoe UI,sans-serif;");
        html.append("max-width:480px;margin:60px auto;padding:24px;color:#222;}");
        html.append("h1{font-size:1.4em;margin:0 0 16px;}");
        html.append("p{line-height:1.5;}");
        html.append(".buttons{margin-top:28px;display:flex;gap:12px;}");
        html.append("button{flex:1;padding:12px;border:0;border-radius:6px;font-size:1em;cursor:pointer;}");
        html.append(".approve{background:#1565c0;color:#fff;}");
        html.append(".deny{background:#eee;color:#333;}");
        html.append("</style></head><body>");
        html.append("<h1>Authorization request</h1>");
        html.append("<p><strong>").append(htmlEscape(clientName)).append("</strong> ");
        html.append("is requesting access to your lsFusion account.</p>");
        html.append("<form method=\"POST\" action=\"").append(htmlEscape(request.getRequestURI())).append("\">");
        // Hidden fields preserve OAuth state across the consent POST.
        appendHidden(html, request, "client_id");
        appendHidden(html, request, "redirect_uri");
        appendHidden(html, request, "response_type");
        appendHidden(html, request, "code_challenge");
        appendHidden(html, request, "code_challenge_method");
        appendHidden(html, request, "state");
        appendHidden(html, request, "scope");
        // CSRF guard: nonce stored in session, validated on POST. Without this, an attacker
        // could forge a top-level navigation /oauth/authorize?...&consent=approve and ride
        // the victim's session cookie to extract a code on their own redirect_uri.
        html.append("<input type=\"hidden\" name=\"").append(CONSENT_NONCE_PARAM)
                .append("\" value=\"").append(htmlEscape(nonce)).append("\">");
        html.append("<div class=\"buttons\">");
        html.append("<button type=\"submit\" name=\"consent\" value=\"deny\" class=\"deny\">Deny</button>");
        html.append("<button type=\"submit\" name=\"consent\" value=\"approve\" class=\"approve\">Approve</button>");
        html.append("</div></form></body></html>");

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/html; charset=utf-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        byte[] bytes = html.toString().getBytes(StandardCharsets.UTF_8);
        response.setContentLength(bytes.length);
        try (OutputStream os = response.getOutputStream()) {
            os.write(bytes);
        }
    }

    /**
     * Generate a fresh consent nonce, stash it in session, return it for embedding in the
     * form. Each consent-form render mints a new value; we don't rotate per-client because
     * the value is per-session (one consent dialog at a time per browser tab is normal).
     */
    private static String mintConsentNonce(HttpServletRequest request) {
        byte[] bytes = new byte[24];
        RNG.nextBytes(bytes);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        request.getSession(true).setAttribute(CONSENT_NONCE_ATTR, nonce);
        return nonce;
    }

    /**
     * Single-use consume: validate the inbound nonce against the session-stored one, clear
     * regardless of outcome (so a leaked / replayed nonce can't be used twice).
     * Returns {@code true} on match.
     */
    private static boolean consumeConsentNonce(HttpServletRequest request, String submitted) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        Object stored = session.getAttribute(CONSENT_NONCE_ATTR);
        session.removeAttribute(CONSENT_NONCE_ATTR);
        return stored != null && submitted != null && stored.equals(submitted);
    }

    private static void appendHidden(StringBuilder html, HttpServletRequest request, String name) {
        String v = request.getParameter(name);
        if (v == null || v.isEmpty()) return;
        html.append("<input type=\"hidden\" name=\"").append(name)
                .append("\" value=\"").append(htmlEscape(v)).append("\">");
    }

    private static String htmlEscape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8.name());
        } catch (java.io.UnsupportedEncodingException e) {
            // UTF-8 is mandatory in any JRE; this branch is unreachable.
            throw new RuntimeException(e);
        }
    }
}
