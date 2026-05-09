package lsfusion.interop.oauth;

/**
 * Wire-stable operation names used between web-client OAuth controllers and the
 * server-side {@code OAuthDispatcher}. Lives in the {@code api} module because both sides
 * need them and {@code web-client} doesn't depend on {@code server}.
 *
 * <p>These strings are part of the {@link lsfusion.interop.logics.remote.RemoteLogicsInterface#oauth}
 * contract — adding/renaming an operation is a versioned change to that contract.
 */
public final class OAuthOperations {

    /** RFC 7591 §3 — register a new OAuth client and return its metadata + client_id. */
    public static final String REGISTER_CLIENT = "registerClient";
    /** Lookup-only by client_id — returns the client's safe-to-display metadata. */
    public static final String GET_CLIENT = "getClient";
    /** Issue an authorization code after the user logged in via /oauth/authorize. */
    public static final String ISSUE_AUTH_CODE = "issueAuthCode";
    /** RFC 6749 §4.1.3 — exchange auth code + PKCE verifier for an access JWT + refresh token. */
    public static final String EXCHANGE_CODE = "exchangeCode";
    /** RFC 6749 §6 — exchange refresh token for a fresh access JWT. */
    public static final String REFRESH_TOKEN = "refreshToken";
    /** RFC 7009 — mark a refresh token as revoked. */
    public static final String REVOKE_TOKEN = "revokeToken";
    /** Pre-dispatch JWT signature/expiry check used by protected resources (e.g. /mcp) to
     *  trigger 401 + WWW-Authenticate on any invalid bearer token, not just absent ones. */
    public static final String VALIDATE_TOKEN = "validateToken";

    private OAuthOperations() {} // not instantiable
}
