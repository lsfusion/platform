package lsfusion.server.physics.admin.oauth;

import lsfusion.interop.base.exception.AuthenticationException;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.oauth.OAuthOperations;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.authentication.AuthenticationLogicsModule;
import lsfusion.server.physics.admin.authentication.controller.remote.RemoteConnection;
import lsfusion.server.physics.admin.authentication.security.controller.manager.SecurityManager;
import lsfusion.server.physics.admin.log.ServerLoggers;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;

/**
 * Server-side dispatcher for the OAuth Authorization Server role.
 *
 * <p>lsFusion plays two OAuth-related roles: as a <em>client</em> of external IdPs (Google,
 * GitHub, …, configured via the {@code OAuth2} class and the existing {@code <oauth2-login>}
 * Spring config) and — implemented here — as an <em>authorization server</em> issuing tokens
 * for external applications (MCP connectors, mobile apps, integrations) that then call back
 * into our APIs under the user's identity. The two roles share JWT plumbing (signing secret,
 * {@code SecurityManager.generateToken/parseToken}) but otherwise live in separate code
 * paths; this dispatcher owns only the server role.
 *
 * <p>Single RMI entry point (per
 * {@link lsfusion.interop.logics.remote.RemoteLogicsInterface#oauth}): a string-coded
 * {@code operation} plus a JSON request body. Persistence is split by lifetime:
 * {@code OAuthClient} and {@code OAuthRefreshToken} live in lsFusion classes (declared in
 * {@code Authentication.lsf}, accessed through {@code DataSession} so multi-web-client
 * deployments share state via the app server); short-lived 10-min auth codes live in
 * app-server JVM memory via {@link OAuthAuthCodeManager}. Each operation is implemented
 * in a separate method below — the dispatch step here just routes by name and uniformly
 * maps thrown {@link OAuthException}s to RFC 6749 §5.2
 * {@code {"error": ..., "error_description": ...}} envelopes.
 */
public class OAuthDispatcher {

    // Operation names live in {@link OAuthOperations} (api module) — see that class for
    // the wire-stable strings; server- and web-client sides reference the same constants.

    // TTLs are admin-tunable via {@link Settings} — no rebuild needed when an incident
    // calls for shorter access tokens or longer refresh windows.

    /** Length of the random string component for client_id / refresh_token / auth_code values. */
    private static final int OPAQUE_TOKEN_BYTES = 32;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final BusinessLogics businessLogics;
    private final SecurityManager securityManager;
    private final OAuthAuthCodeManager authCodeManager;

    public OAuthDispatcher(BusinessLogics businessLogics, SecurityManager securityManager) {
        this.businessLogics = businessLogics;
        this.securityManager = securityManager;
        this.authCodeManager = new OAuthAuthCodeManager();
    }

    /**
     * Route one operation. Returns the operation-specific JSON response on success or an
     * RFC 6749 §5.2 error envelope ({@code error} + optional {@code error_description}) on
     * any {@link OAuthException}. Other exceptions surface as {@code server_error}.
     */
    public String dispatch(AuthenticationToken token, String operation, String requestJson) {
        try {
            JSONObject request = requestJson == null || requestJson.isEmpty()
                    ? new JSONObject()
                    : new JSONObject(requestJson);

            JSONObject response;
            switch (operation) {
                case OAuthOperations.REGISTER_CLIENT: response = registerClient(request); break;
                case OAuthOperations.GET_CLIENT:      response = getClient(request); break;
                case OAuthOperations.ISSUE_AUTH_CODE: response = issueAuthCode(token, request); break;
                case OAuthOperations.EXCHANGE_CODE:   response = exchangeCode(request); break;
                case OAuthOperations.REFRESH_TOKEN:   response = refreshToken(request); break;
                case OAuthOperations.REVOKE_TOKEN:    response = revokeToken(request); break;
                case OAuthOperations.VALIDATE_TOKEN:  response = validateToken(token, request); break;
                default:
                    throw new OAuthException("invalid_request", "Unknown OAuth operation: " + operation);
            }
            return response.toString();
        } catch (OAuthException e) {
            return errorEnvelope(e.error, e.errorDescription).toString();
        } catch (Exception e) {
            // Surface the raw exception message — the primary consumer of these OAuth
            // endpoints is an AI agent that benefits from detail (a generic "internal error"
            // gives nothing to act on). The trade-off (potential SQL/internals leak in
            // error envelopes) is acceptable given the deployment model.
            ServerLoggers.systemLogger.warn("OAuth " + operation + " failed", e);
            return errorEnvelope("server_error", e.getMessage()).toString();
        }
    }

    // ── operations ───────────────────────────────────────────────────────────

    /**
     * RFC 7591 §3: register a new client and return its metadata. MVP supports only public
     * clients ({@code token_endpoint_auth_method=none}, PKCE-only auth) — any other value
     * is rejected with {@code invalid_client_metadata}, since confidential-client auth on
     * the token endpoint isn't implemented yet and silently issuing a secret nobody verifies
     * would be worse than not supporting them at all.
     *
     * <p>{@code redirect_uris} is required and stored verbatim (newline-separated under the
     * hood, with CR/LF rejected per-URI to prevent split-injection); later /authorize calls
     * compare the requested {@code redirect_uri} against this list with strict string
     * equality, per RFC 6749 §3.1.2.3.
     */
    private JSONObject registerClient(JSONObject request) throws SQLException, SQLHandledException {
        String clientName = request.optString("client_name", "").trim();
        if (clientName.isEmpty()) {
            throw new OAuthException("invalid_request", "client_name is required");
        }
        JSONArray redirectUrisArr = request.optJSONArray("redirect_uris");
        if (redirectUrisArr == null || redirectUrisArr.length() == 0) {
            throw new OAuthException("invalid_request", "redirect_uris must be a non-empty array");
        }
        StringBuilder redirectURIs = new StringBuilder();
        for (int i = 0; i < redirectUrisArr.length(); i++) {
            String uri = redirectUrisArr.optString(i, "").trim();
            if (uri.isEmpty()) {
                throw new OAuthException("invalid_redirect_uri", "redirect_uris[" + i + "] is empty");
            }
            // We persist the list as newline-separated, so a CR/LF inside any single URI
            // would let an attacker forge multiple "registered" entries from one input.
            // Reject up front. (Per RFC 3986, CR/LF have no place in a URI anyway.)
            if (uri.indexOf('\n') >= 0 || uri.indexOf('\r') >= 0) {
                throw new OAuthException("invalid_redirect_uri",
                        "redirect_uris[" + i + "] must not contain CR/LF");
            }
            // Per RFC 6749 §3.1.2: redirect_uri MUST be absolute (scheme + authority), MUST
            // NOT contain a fragment, and MUST use a scheme the server controls. We require
            // http or https specifically — these are the only schemes claude.ai / Cursor /
            // Claude Desktop send (and the only ones a browser can reasonably redirect to).
            // Rejecting opaque/relative URIs here means /authorize-time can build redirect
            // URLs with a confidence we have scheme://authority/path to work with.
            try {
                java.net.URI parsed = new java.net.URI(uri);
                if (parsed.getFragment() != null) {
                    throw new OAuthException("invalid_redirect_uri",
                            "redirect_uris[" + i + "] must not contain a fragment");
                }
                String scheme = parsed.getScheme();
                if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                    throw new OAuthException("invalid_redirect_uri",
                            "redirect_uris[" + i + "] must be an absolute http or https URI");
                }
                if (parsed.getAuthority() == null || parsed.getAuthority().isEmpty()) {
                    throw new OAuthException("invalid_redirect_uri",
                            "redirect_uris[" + i + "] must include an authority (host)");
                }
            } catch (java.net.URISyntaxException e) {
                throw new OAuthException("invalid_redirect_uri",
                        "redirect_uris[" + i + "] is not a valid URI: " + e.getMessage());
            }
            if (i > 0) redirectURIs.append('\n');
            redirectURIs.append(uri);
        }

        // MVP supports only public clients (token_endpoint_auth_method=none, PKCE-only auth).
        // This matches MCP web connectors (claude.ai, Cursor, Claude Desktop) which never
        // hold a shared secret. Confidential clients require client-auth checks on
        // exchangeCode/refreshToken/revokeToken — that path will be added once we have a
        // real use case for it; until then accepting client_secret_basic at registration
        // would issue a secret nobody actually verifies, which is worse than rejecting it.
        String authMethod = request.optString("token_endpoint_auth_method", "none");
        if (!"none".equals(authMethod)) {
            throw new OAuthException("invalid_client_metadata",
                    "Only token_endpoint_auth_method=none (public clients with PKCE) is supported");
        }

        String clientId = randomToken(OPAQUE_TOKEN_BYTES);
        LocalDateTime nowDt = nowSeconds();

        try (DataSession session = createSession()) {
            AuthenticationLogicsModule lm = getAuthLM();
            DataObject clientObj = session.addObject(lm.oauthClient);
            lm.clientIdOAuthClient.change(clientId, session, clientObj);
            lm.clientNameOAuthClient.change(clientName, session, clientObj);
            lm.redirectURIsOAuthClient.change(redirectURIs.toString(), session, clientObj);
            // {@code trusted} stays null → consent screen shown on first authorize. Admin
            // promotes specific clients via the lsFusion form.
            lm.createdAtOAuthClient.change(nowDt, session, clientObj);
            apply(session);
        }

        // Per RFC 7591 §3.2.1, the registration response MUST include the registered metadata
        // verbatim (so the client can verify the server did not silently rewrite it) plus the
        // server-issued credentials.
        return new JSONObject()
                .put("client_id", clientId)
                .put("client_id_issued_at", System.currentTimeMillis() / 1000)
                .put("client_name", clientName)
                .put("redirect_uris", redirectUrisArr)
                .put("token_endpoint_auth_method", "none")
                .put("grant_types", new JSONArray().put("authorization_code").put("refresh_token"))
                .put("response_types", new JSONArray().put("code"));
    }

    /**
     * Lookup-only by {@code client_id}: returns the client's safe-to-display metadata
     * (no secret) including its registered {@code redirect_uris}. On unknown client throws
     * {@code invalid_client} per RFC 6749 §5.2.
     *
     * <p>This intentionally does NOT validate any specific {@code redirect_uri} — the web
     * tier's {@code /oauth/authorize} controller MUST do that itself before showing a login
     * or consent screen, by string-matching the inbound parameter against the
     * {@code redirect_uris} array in the response. Mismatch must be a direct error response,
     * not a redirect (we don't yet know which URI is trustable). The dispatcher's later
     * {@link #issueAuthCode} call re-validates as defense in depth, but treating that as
     * the only check would let the user spend a login round-trip on a request that's
     * already invalid.
     */
    private JSONObject getClient(JSONObject request) throws SQLException, SQLHandledException {
        String clientId = requireString(request, "client_id");
        try (DataSession session = createSession()) {
            DataObject clientObj = lookupClient(session, clientId);
            return clientView(session, clientObj);
        }
    }

    /**
     * Called from {@code /oauth/authorize} immediately after the user's session-cookie auth
     * succeeded (the web controller already let Spring Security's filter chain run, so
     * {@code userToken} is now a real JWT, not anonymous). Stores an entry in
     * {@link OAuthAuthCodeManager} tying together client + user + redirect_uri + PKCE
     * challenge, and returns the opaque code for the controller to embed in the redirect.
     *
     * <p>{@code redirect_uri} is validated against the client's registered list with strict
     * string match. PKCE is mandatory: a missing {@code code_challenge} (or unsupported
     * {@code code_challenge_method}) is rejected — we don't fall back to plain method since
     * MCP clients always use S256 and weakening the policy invites downgrade attacks.
     */
    private JSONObject issueAuthCode(AuthenticationToken userToken, JSONObject request)
            throws SQLException, SQLHandledException {
        if (userToken == null || userToken.isAnonymous()) {
            // Should not happen — controller calls this only after Spring Security verified
            // an authenticated session — but defend in depth.
            throw new OAuthException("access_denied", "Authentication required");
        }
        String userLogin = securityManager.parseToken(userToken);
        if (userLogin == null) {
            throw new OAuthException("access_denied", "Authentication required");
        }
        String clientId = requireString(request, "client_id");
        String redirectURI = requireString(request, "redirect_uri");
        String codeChallenge = requireString(request, "code_challenge");
        String codeChallengeMethod = request.optString("code_challenge_method", "S256");
        if (!"S256".equals(codeChallengeMethod)) {
            throw new OAuthException("invalid_request",
                    "Only code_challenge_method=S256 is supported");
        }

        // Resolve the FK objects in a (read-only) session, then hand them to the in-memory
        // {@link OAuthAuthCodeManager} along with the freshly minted code. No DataSession
        // write happens here — auth codes live in process memory only.
        String code;
        try (DataSession session = createSession()) {
            DataObject clientObj = lookupClient(session, clientId);
            requireRedirectURIAllowed(session, clientObj, redirectURI);

            DataObject userObj = securityManager.readUser(userLogin, session);
            if (userObj == null) {
                throw new OAuthException("access_denied", "User not found: " + userLogin);
            }

            code = randomToken(OPAQUE_TOKEN_BYTES);
            int authCodeMinutes = readExpirationMinutes(session, getAuthLM().oauthAuthCodeExpiration,
                    Settings.get().getOauthAuthCodeExpiration());
            long expiresAtMs = System.currentTimeMillis() + authCodeMinutes * 60_000L;
            authCodeManager.put(code, clientObj, userObj, codeChallenge, redirectURI, expiresAtMs);
        }
        return new JSONObject().put("code", code);
    }

    /**
     * RFC 6749 §4.1.3 — exchange a one-time {@code code} (with PKCE {@code code_verifier})
     * for an access JWT plus a fresh refresh token. Single-use per RFC 6749 §10.5: the code
     * is removed atomically from {@link OAuthAuthCodeManager} regardless of whether the
     * subsequent validation succeeds, so a leaked code can't be replayed. Validates, in
     * order: PKCE ({@code SHA-256(code_verifier)} base64url-encoded == stored
     * {@code code_challenge}), {@code redirect_uri} matches the binding,
     * {@code client_id} matches the binding. Failure of any check returns
     * {@code invalid_grant} (the catch-all for code/token verification failures, per
     * RFC 6749 §5.2).
     */
    private JSONObject exchangeCode(JSONObject request) throws SQLException, SQLHandledException {
        String code = requireString(request, "code");
        String codeVerifier = requireString(request, "code_verifier");
        String clientId = requireString(request, "client_id");
        String redirectURI = requireString(request, "redirect_uri");

        // Single-use atomic consume: removes the entry from the in-memory store regardless
        // of subsequent validation outcome, so a leaked code can't be replayed.
        OAuthAuthCodeManager.Entry codeEntry = authCodeManager.consume(code);
        if (codeEntry == null) {
            throw new OAuthException("invalid_grant", "Unknown, expired, or already-used authorization code");
        }
        // PKCE verification: base64url(SHA-256(code_verifier)) == stored code_challenge.
        if (codeEntry.codeChallenge == null || !codeEntry.codeChallenge.equals(pkceS256(codeVerifier))) {
            throw new OAuthException("invalid_grant", "PKCE verification failed");
        }
        if (!redirectURI.equals(codeEntry.redirectURI)) {
            throw new OAuthException("invalid_grant", "redirect_uri does not match the one used at /authorize");
        }

        AuthenticationLogicsModule lm = getAuthLM();
        try (DataSession session = createSession()) {
            // Validate client_id binding using the persisted client object.
            String codeClientId = (String) lm.clientIdOAuthClient.read(session, codeEntry.client);
            if (!clientId.equals(codeClientId)) {
                throw new OAuthException("invalid_grant", "code does not belong to this client");
            }
            String userLogin = (String) lm.loginCustomUser.read(session, codeEntry.user);

            String refreshToken = randomToken(OPAQUE_TOKEN_BYTES);
            DataObject rtObj = session.addObject(lm.oauthRefreshToken);
            lm.tokenOAuthRefreshToken.change(refreshToken, session, rtObj);
            lm.clientOAuthRefreshToken.change(codeEntry.client, session, rtObj);
            lm.userOAuthRefreshToken.change(codeEntry.user, session, rtObj);
            int refreshMinutes = readExpirationMinutes(session, lm.oauthRefreshTokenExpiration,
                    Settings.get().getOauthRefreshTokenExpiration());
            lm.expiresAtOAuthRefreshToken.change(
                    nowSeconds().plusMinutes(refreshMinutes), session, rtObj);
            apply(session);

            return tokenResponse(session, userLogin, refreshToken);
        }
    }

    /**
     * RFC 6749 §6 — exchange a non-revoked, unexpired refresh token for a fresh access JWT.
     * Rotates the refresh token per OAuth 2.1 §4.13.2: each refresh issues a new
     * {@code refresh_token} and revokes the prior one in the same transaction. Replay of
     * the old (now revoked) token surfaces as {@code invalid_grant} on the next attempt —
     * a leaked-token signal the caller can react to. The {@code client_id} parameter is
     * verified against the binding to keep a stolen refresh token from being used by a
     * different registered client.
     */
    private JSONObject refreshToken(JSONObject request) throws SQLException, SQLHandledException {
        String oldRefreshToken = requireString(request, "refresh_token");
        String clientId = requireString(request, "client_id");

        AuthenticationLogicsModule lm = getAuthLM();
        try (DataSession session = createSession()) {
            DataObject oldRtObj = lookupRefreshToken(session, oldRefreshToken);

            DataObject rtClient = (DataObject) lm.clientOAuthRefreshToken.readClasses(session, oldRtObj);
            String rtClientId = (String) lm.clientIdOAuthClient.read(session, rtClient);
            if (!clientId.equals(rtClientId)) {
                throw new OAuthException("invalid_grant", "refresh_token does not belong to this client");
            }
            LocalDateTime expiresAt = (LocalDateTime) lm.expiresAtOAuthRefreshToken.read(session, oldRtObj);
            if (expiresAt == null || expiresAt.isBefore(LocalDateTime.now())) {
                throw new OAuthException("invalid_grant", "refresh_token expired");
            }
            if (lm.revokedAtOAuthRefreshToken.read(session, oldRtObj) != null) {
                throw new OAuthException("invalid_grant", "refresh_token revoked");
            }

            DataObject userObj = (DataObject) lm.userOAuthRefreshToken.readClasses(session, oldRtObj);
            String userLogin = (String) lm.loginCustomUser.read(session, userObj);

            // Rotate: revoke the old token and issue a fresh one atomically. Sliding expiry —
            // each refresh resets the TTL so an actively-used session doesn't get logged out.
            lm.revokedAtOAuthRefreshToken.change(nowSeconds(), session, oldRtObj);

            String newRefreshToken = randomToken(OPAQUE_TOKEN_BYTES);
            DataObject newRtObj = session.addObject(lm.oauthRefreshToken);
            lm.tokenOAuthRefreshToken.change(newRefreshToken, session, newRtObj);
            lm.clientOAuthRefreshToken.change(rtClient, session, newRtObj);
            lm.userOAuthRefreshToken.change(userObj, session, newRtObj);
            int refreshMinutes = readExpirationMinutes(session, lm.oauthRefreshTokenExpiration,
                    Settings.get().getOauthRefreshTokenExpiration());
            lm.expiresAtOAuthRefreshToken.change(
                    nowSeconds().plusMinutes(refreshMinutes), session, newRtObj);
            apply(session);

            return tokenResponse(session, userLogin, newRefreshToken);
        }
    }

    /**
     * RFC 7009 — mark a refresh token as revoked (so subsequent {@code refreshToken}
     * operations on it return {@code invalid_grant}). Per spec, an unknown/already-revoked
     * token also returns success: revocation is idempotent and we don't leak whether the
     * token ever existed. Access JWTs are out of scope here — they expire on their own
     * and aren't tracked, which is the standard JWT trade-off.
     */
    private JSONObject revokeToken(JSONObject request) throws SQLException, SQLHandledException {
        String tokenStr = request.optString("token", "");
        if (tokenStr.isEmpty()) {
            // Per RFC 7009 §2.1 a missing token is invalid_request.
            throw new OAuthException("invalid_request", "token is required");
        }

        try (DataSession session = createSession()) {
            AuthenticationLogicsModule lm = getAuthLM();
            ObjectValue rtVal = lm.oauthRefreshTokenByToken.readClasses(session, new DataObject(tokenStr));
            if (!rtVal.isNull()) {
                lm.revokedAtOAuthRefreshToken.change(nowSeconds(), session, (DataObject) rtVal);
                apply(session);
            }
        }
        return new JSONObject(); // Empty body per RFC 7009 §2.2.
    }

    /**
     * Pre-dispatch token validation used by protected resources (e.g. {@code /mcp}) to
     * decide whether to challenge with 401 + {@code WWW-Authenticate} (which drives MCP
     * OAuth discovery) or let the request through. Centralizes the policy server-side so
     * the web tier doesn't need to read server settings or the JWT secret.
     *
     * <p>Delegates the {@code enableAPI} policy (0 = disabled, 1 = auth required,
     * 2 = anonymous allowed) to {@link RemoteConnection#checkAPIAccess} so {@code /eval}
     * and this pre-gate share a single source of truth — adding {@code enableAPI=3} or
     * tweaking the anonymous rule would mean changing one place. Per-tool MCP gates
     * are no longer needed: this pre-gate runs uniformly for every {@code /mcp} request,
     * so the {@code MCPDispatcher} can dispatch tools without re-checking.
     *
     * <p>The web tier passes {@code "header_present": true|false} because
     * {@link AuthenticationToken#isAnonymous()} alone can't tell {@code Authorization:
     * Bearer anonymous} (a malformed bearer that happens to match the anonymous-token
     * string) from a truly missing header — both surface as
     * {@link AuthenticationToken#ANONYMOUS}. A header-present anonymous-string token is
     * always rejected as a bogus bearer; a missing header falls through to the enableAPI gate.
     *
     * <p>For non-anonymous tokens we additionally verify the JWT via
     * {@link SecurityManager#parseToken} — {@link
     * lsfusion.http.authentication.LSFAuthTokenFilter} wraps the raw header bytes into a
     * non-anonymous {@code AuthenticationToken} without verifying anything, so without this
     * server-side parse a malformed bearer would silently slip past as "non-anonymous".
     */
    private JSONObject validateToken(AuthenticationToken token, JSONObject request) {
        boolean headerPresent = request.optBoolean("header_present", false);

        // Authorization header was sent but produced an anonymous-string token — that's a
        // malformed bearer (e.g. "Bearer anonymous"), never a legitimate auth attempt.
        if (headerPresent && (token == null || token.isAnonymous())) {
            throw new OAuthException("invalid_token", "Bearer is anonymous-string token");
        }

        // Canonical enableAPI gate — same 0/1/2 semantics as /eval.
        try {
            RemoteConnection.checkAPIAccess(token, false, false);
        } catch (AuthenticationException e) {
            throw new OAuthException("invalid_token", "Anonymous token");
        } catch (RuntimeException e) {
            // enableAPI=0 surfaces as "Api is disabled..." — pass the message through so the
            // agent can distinguish "API disabled" from "auth required".
            throw new OAuthException("invalid_token", e.getMessage());
        }

        if (token.isAnonymous()) {
            return new JSONObject().put("user", "anonymous");
        }

        try {
            String userLogin = securityManager.parseToken(token);
            if (userLogin == null) {
                throw new OAuthException("invalid_token", "Token validation returned null");
            }
            return new JSONObject().put("user", userLogin);
        } catch (OAuthException e) {
            throw e;
        } catch (Exception e) {
            // Pass the parser's actual message through to the agent — knowing whether the
            // failure was signature mismatch, expiry, malformed structure, or an unknown
            // claim shape lets it choose between "refresh and retry" vs "re-run discovery".
            // Same trade-off as the catch-all in {@link #dispatch}: AI-agent diagnostics
            // beat sanitized opacity in this deployment.
            throw new OAuthException("invalid_token", e.getMessage());
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private DataSession createSession() throws SQLException {
        return ThreadLocalContext.getDbManager().createSession();
    }

    /**
     * Commit the session through the normal apply pipeline. Mirrors {@code LogicsManager.apply}
     * but inlined here so the dispatcher doesn't have to be a Spring-managed bean.
     */
    private void apply(DataSession session) throws SQLException, SQLHandledException {
        session.applyException(businessLogics, ThreadLocalContext.getStack());
    }

    private AuthenticationLogicsModule getAuthLM() {
        return businessLogics.authenticationLM;
    }

    /**
     * Lookup an OAuthClient by its public {@code clientId} or throw {@code invalid_client}.
     * Used everywhere we need to resolve a client_id parameter into a database row.
     */
    private DataObject lookupClient(DataSession session, String clientId) throws SQLException, SQLHandledException {
        AuthenticationLogicsModule lm = getAuthLM();
        ObjectValue val = lm.oauthClientByClientId.readClasses(session, new DataObject(clientId));
        if (val.isNull()) {
            throw new OAuthException("invalid_client", "Unknown client_id");
        }
        return (DataObject) val;
    }

    /** Lookup an OAuthRefreshToken by its opaque {@code token} string or throw {@code invalid_grant}. */
    private DataObject lookupRefreshToken(DataSession session, String token) throws SQLException, SQLHandledException {
        AuthenticationLogicsModule lm = getAuthLM();
        ObjectValue val = lm.oauthRefreshTokenByToken.readClasses(session, new DataObject(token));
        if (val.isNull()) {
            throw new OAuthException("invalid_grant", "Unknown refresh token");
        }
        return (DataObject) val;
    }

    /**
     * Read the client's registered redirect_uris from DB and parse the newline-separated
     * storage format into a list of trimmed, non-empty entries. Centralizes the parse so
     * {@link #requireRedirectURIAllowed} and {@link #clientView} stay in sync.
     */
    private List<String> readRedirectURIs(DataSession session, DataObject clientObj)
            throws SQLException, SQLHandledException {
        String stored = (String) getAuthLM().redirectURIsOAuthClient.read(session, clientObj);
        if (stored == null || stored.isEmpty()) return java.util.Collections.emptyList();
        List<String> result = new java.util.ArrayList<>();
        for (String line : stored.split("\n")) {
            String t = line.trim();
            if (!t.isEmpty()) result.add(t);
        }
        return result;
    }

    /**
     * Validate that {@code redirectURI} is one of the URIs registered for the client. Strict
     * string match per RFC 6749 §3.1.2.3 — no normalization, no wildcards.
     */
    private void requireRedirectURIAllowed(DataSession session, DataObject clientObj, String redirectURI)
            throws SQLException, SQLHandledException {
        List<String> registered = readRedirectURIs(session, clientObj);
        if (registered.isEmpty()) {
            throw new OAuthException("invalid_request", "Client has no registered redirect_uris");
        }
        if (!registered.contains(redirectURI)) {
            throw new OAuthException("invalid_request", "redirect_uri is not registered for this client");
        }
    }

    /**
     * Public-safe view of an OAuthClient row — includes everything the caller of
     * {@link #getClient} needs to render the consent / authorize page, but never the
     * client_secret.
     */
    private JSONObject clientView(DataSession session, DataObject clientObj) throws SQLException, SQLHandledException {
        AuthenticationLogicsModule lm = getAuthLM();
        JSONArray redirectURIs = new JSONArray();
        for (String uri : readRedirectURIs(session, clientObj)) redirectURIs.put(uri);
        Boolean trusted = (Boolean) lm.trustedOAuthClient.read(session, clientObj);
        return new JSONObject()
                .put("client_id", lm.clientIdOAuthClient.read(session, clientObj))
                .put("client_name", nvl((String) lm.clientNameOAuthClient.read(session, clientObj), ""))
                .put("redirect_uris", redirectURIs)
                .put("trusted", trusted != null && trusted);
    }

    /**
     * Build the standard RFC 6749 §5.1 token response: {@code access_token}, {@code token_type},
     * {@code expires_in}, {@code refresh_token}. Used by both {@link #exchangeCode} and
     * {@link #refreshToken}, which differ only in whether the refresh token is freshly issued
     * or carried forward.
     */
    private JSONObject tokenResponse(DataSession session, String userLogin, String refreshToken)
            throws SQLException, SQLHandledException {
        int accessTtl = readExpirationMinutes(session, getAuthLM().oauthAccessTokenExpiration,
                Settings.get().getOauthAccessTokenExpiration());
        AuthenticationToken accessToken = securityManager.generateToken(userLogin, accessTtl, false);
        return new JSONObject()
                .put("access_token", accessToken.string)
                .put("token_type", "Bearer")
                .put("expires_in", accessTtl * 60)
                .put("refresh_token", refreshToken);
    }

    /**
     * Read an admin-tunable expiration (minutes) from the lsf settings form, falling back to
     * the {@link Settings} compile-time default when the form value is null. A fresh DB has
     * the form fields empty — we don't seed them — so this fallback is what makes the system
     * work out of the box; admin overrides take effect on next read.
     */
    private int readExpirationMinutes(DataSession session, LP<?> property, int defaultMinutes)
            throws SQLException, SQLHandledException {
        Object v = property.read(session);
        return v != null ? ((Number) v).intValue() : defaultMinutes;
    }

    /**
     * Generate a URL-safe random opaque token of the given byte-strength, base64url-encoded
     * without padding. Strength here is the underlying byte count: 32 bytes ⇒ 256 bits of
     * entropy ⇒ a 43-character string after base64url encoding.
     */
    private static String randomToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(bytes);
        return URL_ENCODER.encodeToString(bytes);
    }

    /**
     * RFC 7636 §4.2 PKCE S256 transform: {@code base64url(SHA-256(code_verifier))}.
     * Produced by the client at /authorize as {@code code_challenge}, and reproduced here at
     * /token from the {@code code_verifier} the client sends, then compared by string equality.
     */
    private static String pkceS256(String codeVerifier) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return URL_ENCODER.encodeToString(hash);
        } catch (Exception e) {
            // SHA-256 is mandatory in any JRE; this branch is unreachable.
            throw new RuntimeException(e);
        }
    }

    /**
     * Current time as a {@link LocalDateTime} truncated to second precision — the lsFusion
     * {@code DATA DATETIME} class stores second-precision {@link LocalDateTime} values (see
     * {@link lsfusion.server.logics.classes.data.time.DateTimeClass#read}), so writing
     * anything finer (e.g. a millisecond-precision Timestamp) trips the
     * {@code DataObject.<init>} {@code read(written).equals(written)} invariant.
     * Same constraint on read-back: callers cast to {@code LocalDateTime}, not Timestamp.
     */
    private static LocalDateTime nowSeconds() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    private static String requireString(JSONObject request, String key) {
        String v = request.optString(key, "");
        if (v.isEmpty()) {
            throw new OAuthException("invalid_request", key + " is required");
        }
        return v;
    }

    private static String nvl(String s, String fallback) {
        return s != null ? s : fallback;
    }

    private static JSONObject errorEnvelope(String error, String errorDescription) {
        JSONObject env = new JSONObject().put("error", error);
        if (errorDescription != null && !errorDescription.isEmpty()) {
            env.put("error_description", errorDescription);
        }
        return env;
    }

    /**
     * Unchecked throw inside operation methods — the dispatcher's catch maps it to an
     * RFC 6749 §5.2 error envelope. {@code error} should be one of the standard codes:
     * {@code invalid_request}, {@code invalid_client}, {@code invalid_grant},
     * {@code unauthorized_client}, {@code unsupported_grant_type}, {@code invalid_scope},
     * {@code server_error} (or — only at registration — {@code invalid_redirect_uri},
     * {@code invalid_client_metadata} from RFC 7591 §3.2.2).
     */
    public static class OAuthException extends RuntimeException {
        public final String error;
        public final String errorDescription;

        public OAuthException(String error, String errorDescription) {
            super(error + ": " + errorDescription);
            this.error = error;
            this.errorDescription = errorDescription;
        }
    }
}
