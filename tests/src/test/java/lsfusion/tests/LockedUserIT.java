package lsfusion.tests;

import lsfusion.base.remote.RMIUtils;
import lsfusion.interop.base.exception.LockedException;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.connection.authentication.OAuth2Authentication;
import lsfusion.interop.logics.remote.RemoteLogicsInterface;
import lsfusion.interop.logics.remote.RemoteLogicsLoaderInterface;
import lsfusion.interop.oauth.OAuthOperations;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/** Users who are locked, on the server of DefaultApiIT, which lets only what an action opens by its annotations in
 *  (enableAPI=0, the default), with a universal password: a locked user gets in by nothing - a password, an OAuth
 *  provider's login, a token, the OAuth server's codes and tokens - and the user every call without a login runs as is
 *  created locked, while a call without a login still runs as it, until an administrator unlocks it. */
public class LockedUserIT {

    static final String UNIVERSAL_PASSWORD = "luUniversal1"; // the server's, see DefaultApiIT
    private static final String EMAIL = "lu@example.com";
    private static final String REDIRECT_URI = "https://localhost/lu";
    private static final String CODE_VERIFIER = "lu-code-verifier-0123456789-0123456789-0123456789";

    private static int httpPort, rmiPort;
    // the user of the calls without a login as a new database has it, before any test changes it
    private static String createdLocked, createdPassword;

    @BeforeClass
    public static void startServer() throws Exception {
        DefaultApiIT.start();
        httpPort = DefaultApiIT.httpPort;
        rmiPort = DefaultApiIT.rmiPort;
        createdLocked = call("luLocked&p=anonymous");
        createdPassword = call("luPassword&p=anonymous");
    }

    // with no password, and a call without a login runs as it
    @Test
    public void theAnonymousUserIsCreatedLocked() throws Exception {
        Assert.assertEquals("locked", createdLocked);
        Assert.assertEquals("none", createdPassword);
        Assert.assertEquals("anonymous", call("luLoginNoauth"));
    }

    // even with the universal password, which lets an administrator in whatever password they have : by the user's login,
    // in another letter case too, and by its e-mail - while a call without a login runs as the user
    @Test
    public void aLockedUserLogsInByNoPassword() throws Exception {
        call("luLock&p=anonymous");
        Assert.assertEquals(EMAIL, call("luSetEmail&p=anonymous&p=" + EMAIL));
        Assert.assertEquals("admin", call("luLogin", "Authorization", basic("admin", UNIVERSAL_PASSWORD)));

        for (String login : new String[] {"anonymous", "Anonymous", EMAIL}) {
            HttpResponse<String> response = post("luLogin", "Authorization", basic(login, UNIVERSAL_PASSWORD));
            Assert.assertEquals(login + " " + response.body(), 401, response.statusCode());
        }
        Assert.assertEquals("anonymous", call("luLoginNoauth"));
    }

    // an OAuth provider's user that finds a locked user, by its e-mail or by its login, is refused, with nothing of the user
    // changed
    @Test
    public void aLockedUserLogsInByNoOAuthProvider() throws Exception {
        call("luLock&p=anonymous");
        Assert.assertEquals(EMAIL, call("luSetEmail&p=anonymous&p=" + EMAIL));
        for (String[] user : new String[][] {{"luProvider:luOAuth", EMAIL}, {"anonymous", "luNobody@example.com"}}) {
            String userName = user[0];
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("email", user[1]);
            userInfo.put("name", "First Last");
            try {
                logics().authenticateUser(new OAuth2Authentication(userName, "secretKey", userInfo));
                Assert.fail("the OAuth provider's user " + userName + " logged in as a locked user");
            } catch (LockedException e) {
                // refused
            }
        }
        Assert.assertEquals("none", call("luFirstName&p=anonymous"));
    }

    // a token given before a user is locked lets the user in no more - by a call that needs a login, and by the check the web
    // server asks of a token before an MCP call - nor does one an application issues for a locked user, or one for no user
    @Test
    public void aLockedUserIsLetInByNoToken() throws Exception {
        call("luNewUser&p=lutoken");
        String token = call("luToken&p=lutoken");
        Assert.assertEquals("lutoken", new JSONObject(validateToken(token)).getString("user"));

        call("luLock&p=lutoken");
        for (String refused : new String[] {token, call("luToken&p=anonymous"), call("luToken&p=Anonymous"), call("luTokenOfNoLogin")}) {
            HttpResponse<String> response = post("luLogin", "Authorization", "Bearer " + refused);
            Assert.assertEquals(response.body(), 401, response.statusCode());
            assertError("invalid_token", new JSONObject(validateToken(refused)));
        }
    }

    // the OAuth server gives a user locked since it logged in no authorization code, and a code or a refresh token given
    // before the lock gives no token; nor does a token that names no user get a code
    @Test
    public void aLockedUserGetsNoOAuthCodeOrToken() throws Exception {
        call("luNewUser&p=luoauth");
        AuthenticationToken token = new AuthenticationToken(call("luToken&p=luoauth"));
        String clientId = oauth(AuthenticationToken.ANONYMOUS, OAuthOperations.REGISTER_CLIENT, new JSONObject()
                .put("client_name", "luClient").put("redirect_uris", new JSONArray().put(REDIRECT_URI))).getString("client_id");
        JSONObject tokens = oauth(AuthenticationToken.ANONYMOUS, OAuthOperations.EXCHANGE_CODE, exchange(issueCode(token, clientId), clientId));
        Assert.assertTrue(tokens.toString(), tokens.has("access_token"));
        String code = issueCode(token, clientId);

        call("luLock&p=luoauth");
        assertError("access_denied", oauth(token, OAuthOperations.ISSUE_AUTH_CODE, codeRequest(clientId)));
        assertError("access_denied", oauth(new AuthenticationToken(call("luTokenOfNoLogin")), OAuthOperations.ISSUE_AUTH_CODE, codeRequest(clientId)));
        assertError("invalid_grant", oauth(AuthenticationToken.ANONYMOUS, OAuthOperations.EXCHANGE_CODE, exchange(code, clientId)));
        assertError("invalid_grant", oauth(AuthenticationToken.ANONYMOUS, OAuthOperations.REFRESH_TOKEN, new JSONObject()
                .put("refresh_token", tokens.getString("refresh_token")).put("client_id", clientId)));
    }

    // a login as the user is an administrator's choice : unlocked with a password, it logs in, and a start of the server keeps
    // it so - the user is created locked, not locked at every start
    @Test
    public void anAdministratorUnlocksTheAnonymousUser() throws Exception {
        try {
            call("luUnlock&p=anonymous&p=luPassword1");
            Assert.assertEquals("anonymous", call("luLogin", "Authorization", basic("anonymous", "luPassword1")));

            DefaultApiIT.restart("target/server-api0-restart.log");
            httpPort = DefaultApiIT.httpPort;
            rmiPort = DefaultApiIT.rmiPort;
            Assert.assertEquals("open", call("luLocked&p=anonymous"));
            Assert.assertEquals("anonymous", call("luLogin", "Authorization", basic("anonymous", "luPassword1")));
        } finally {
            call("luLock&p=anonymous");
        }
    }

    private static RemoteLogicsInterface logics() throws Exception {
        return RMIUtils.<RemoteLogicsLoaderInterface>rmiLookup("localhost", rmiPort, "default", "RemoteLogicsLoader").getLogics();
    }

    // as the web server asks it, for a request with the token in its header
    private static String validateToken(String token) throws Exception {
        return logics().oauth(new AuthenticationToken(token), OAuthOperations.VALIDATE_TOKEN, new JSONObject().put("header_present", true).toString());
    }

    private static JSONObject oauth(AuthenticationToken token, String operation, JSONObject request) throws Exception {
        return new JSONObject(logics().oauth(token, operation, request.toString()));
    }

    // as the web server asks it once the user logged in at /oauth/authorize : the code
    private static String issueCode(AuthenticationToken token, String clientId) throws Exception {
        JSONObject response = oauth(token, OAuthOperations.ISSUE_AUTH_CODE, codeRequest(clientId));
        Assert.assertTrue(response.toString(), response.has("code"));
        return response.getString("code");
    }

    private static JSONObject codeRequest(String clientId) throws Exception {
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(CODE_VERIFIER.getBytes(StandardCharsets.US_ASCII)));
        return new JSONObject().put("client_id", clientId).put("redirect_uri", REDIRECT_URI)
                .put("code_challenge", challenge).put("code_challenge_method", "S256");
    }

    private static JSONObject exchange(String code, String clientId) {
        return new JSONObject().put("code", code).put("code_verifier", CODE_VERIFIER).put("client_id", clientId)
                .put("redirect_uri", REDIRECT_URI);
    }

    private static void assertError(String error, JSONObject response) {
        Assert.assertEquals(response.toString(), error, response.optString("error"));
    }

    private static String basic(String login, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((login + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    // a call that has to run : its answer
    private static String call(String action, String... headers) throws Exception {
        HttpResponse<String> response = post(action, headers);
        Assert.assertEquals(action + ": " + response.body(), 200, response.statusCode());
        return response.body();
    }

    // an action of LockedUser.lsf, with its parameters after it
    private static HttpResponse<String> post(String action, String... headers) throws Exception {
        return TestServer.post(httpPort, "/exec?action=LockedUser." + action, "", "text/plain", headers);
    }
}
