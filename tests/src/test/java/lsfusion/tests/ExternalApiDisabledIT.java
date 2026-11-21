package lsfusion.tests;

import lsfusion.base.BaseUtils;
import lsfusion.base.remote.RMIUtils;
import lsfusion.interop.base.exception.LoginException;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.connection.ComputerInfo;
import lsfusion.interop.connection.ConnectionInfo;
import lsfusion.interop.connection.UserInfo;
import lsfusion.interop.connection.authentication.OAuth2Authentication;
import lsfusion.interop.logics.LogicsSessionObject;
import lsfusion.interop.logics.remote.RemoteLogicsInterface;
import lsfusion.interop.logics.remote.RemoteLogicsLoaderInterface;
import lsfusion.interop.navigator.NavigatorInfo;
import lsfusion.interop.navigator.remote.RemoteNavigatorInterface;
import lsfusion.interop.session.ExternalRequest;
import lsfusion.interop.session.SessionInfo;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lsfusion.tests.TestServer.BASE;

/** The calls the platform itself makes through the external API - its web client, its desktop client, the messengers'
 *  webhooks - on a server of its own that lets the API in only for what an action opens by its annotations
 *  (enableAPI=0, the default): what is called before a login is opened by @@noauth, what a logged in user calls by
 *  @@api. A call the API refused would fail here, where the web client takes a refusal of some of them for an empty
 *  answer. Also the first login of an OAuth provider's user while registration is switched off, and whom the
 *  navigator's settings give the web agent's token. */
public class ExternalApiDisabledIT {

    private static final String DATABASE = "lsfusion_test_api0_" + ProcessHandle.current().pid();

    private static Process server;
    private static int httpPort, rmiPort;

    @BeforeClass
    public static void startServer() throws Exception {
        int[] ports = TestServer.freePorts(4);
        httpPort = ports[0];
        rmiPort = ports[1];
        Path log = BASE.resolve("target/server-api0.log");
        server = TestServer.start(DATABASE, ports, log, BASE.resolve("target/classes"),
                "settings.enableAPI=0",
                // the periodic drop of the values the server keeps, which a change does not drop itself : run as the
                // server starts, then an hour later
                "settings.flushAsyncValuesCaches=3600",
                "lsfusion.server.devmode=false"); // which would set enableAPI to 2 whatever is given
        TestServer.waitStarted(server, log);
    }

    @AfterClass
    public static void stopServer() throws Exception {
        if (server != null)
            TestServer.stop(server, DATABASE);
    }

    // what is called with no user logged in, each by whom : every one of them is run, though an anonymous call of an
    // action that is not opened to it is refused here
    @Test
    public void theCallsBeforeALoginRun() throws Exception {
        // the login page, the registration, password and two-factor pages, before they are drawn
        noauth("Service.getServerSettings[]", "");
        // the password pages : a reset of a user that does not exist, a change by a token that was never given. The web
        // client names the actions that take its json with their signature
        noauth("Authentication.resetPassword[JSONFILE]", "[{\"userNameOrEmail\":\"eadNobody\"}]");
        noauth("Authentication.changePassword[JSONFILE]", "[{\"token\":\"eadNoToken\",\"newPassword\":\"eadPassword1\"}]");
        // the two-factor page, for a user that does not exist
        noauth("Authentication.send2FACode[JSONFILE]", "[{\"username\":\"eadNobody\"}]");
        noauth("Authentication.verify2FACode[JSONFILE]", "[{\"username\":\"eadNobody\",\"code\":\"000000\"}]");
        // the link to the desktop client the login page shows, which the browser follows with no login
        noauth("Security.generateJnlp&p=800m", "");
        // the navigator's settings : asked after the login, though anyone may ask them
        noauth("Service.getClientSettings[]", "");
        noauth("Service.getInitSettings[]", "");
        // the messengers' webhooks, with events of no bot this server has : without the secret of a bot's address the
        // webhook itself answers 403, all but Viber's check of an address it registers
        noauth("Viber.webHookViber", "{\"event\":\"webhook\"}");
        refusedByAction("Telegram.webHookTelegram", "{\"update_id\":1}");
        refusedByAction("Skype.webHookSkype", "{\"action\":\"eadNone\"}");
        refusedByAction("Whatsapp.webHookWhatsapp", "{\"entry\":[]}");
    }

    // what the registration page sends is refused on a new database, and a user registered once an administrator switches
    // registration on gets what a logged in user calls : by @@api, and only that
    @Test
    public void aRegisteredUserGetsTheCallsOfALoggedInUser() throws Exception {
        String user = "[{\"login\":\"eadRegistered\",\"password\":\"eadPassword1\",\"firstName\":\"First\",\"lastName\":\"Last\",\"email\":\"ead@example.com\"}]";
        JSONObject refusal = new JSONObject(noauth("Authentication.registerUser[JSONFILE]", user));
        Assert.assertTrue(refusal.toString(), refusal.has("error") && !refusal.has("success"));

        noauth("HttpRegistration.hrEnable", "");
        try {
            JSONObject registered = new JSONObject(noauth("Authentication.registerUser[JSONFILE]", user));
            Assert.assertTrue(registered.toString(), registered.has("success") && !registered.has("error"));

            String basic = "Basic " + Base64.getEncoder().encodeToString("eadRegistered:eadPassword1".getBytes(StandardCharsets.UTF_8));
            // the token an integration logs in with
            String token = call("Authentication.getAuthToken", "", "Authorization", basic);
            Assert.assertFalse(token, token.isEmpty());
            // what the web client asks right after the login, by the token
            new JSONObject(call("Authentication.getCurrentUserLocale", "", "Authorization", "Bearer " + token));

            HttpResponse<String> anonymous = post("Authentication.getAuthToken", "");
            Assert.assertEquals(anonymous.body(), 401, anonymous.statusCode());
            // actions that are not opened to the API, for the same user : one of the test logics, and the one a link
            // /file/<name> outside the web server's own files is sent to
            for (String action : new String[]{"HttpRegistration.hrNotOpened", "Utils.getResource&p=HttpRegistration.lsf"}) {
                HttpResponse<String> closed = post(action, "", "Authorization", "Bearer " + token);
                Assert.assertTrue(action + ": " + closed.statusCode() + " " + closed.body(), closed.statusCode() != 200 && closed.body().contains("Api is disabled"));
            }
        } finally {
            noauth("HttpRegistration.hrDisable", "");
        }
    }

    // the web client logs a user of an OAuth provider in by the server's authenticateUser : the first login of such a user
    // registers them, and while registration is switched off it is refused, with no user created - until an
    // administrator switches it on, and again once it is switched off
    @Test
    public void theFirstOAuthLoginIsRefusedWhileRegistrationIsDisabled() throws Exception {
        RemoteLogicsInterface logics = RMIUtils.<RemoteLogicsLoaderInterface>rmiLookup("localhost", rmiPort, "default", "RemoteLogicsLoader").getLogics();
        String userName = "eadProvider:eadOAuth";
        OAuth2Authentication login = oauthLogin(userName, "eadoauth@example.com");
        assertRefused(logics, login);

        noauth("HttpRegistration.hrEnable", "");
        try {
            // right after the switch : the refused login left the disabled flag kept by the server, and the switch's
            // apply has to drop it, with no wait for the periodic drop
            AuthenticationToken token = logics.authenticateUser(login);
            Assert.assertFalse(token.isAnonymous());
            Assert.assertTrue(userExists(userName));
            // what the web client asks right after the login, by the token
            new JSONObject(call("Authentication.getCurrentUserLocale", "", "Authorization", "Bearer " + token.string));
        } finally {
            noauth("HttpRegistration.hrDisable", "");
        }

        // right after the switch off : the login above left the server keeping the value that let registration in
        assertRefused(logics, oauthLogin("eadProvider:eadOAuthLater", "eadoauthlater@example.com"));
    }

    // the navigator's settings give the web agent's token to an authenticated call, by the navigator the web client
    // reads them with and by the API, and not to a call made without a login : neither by the API nor by a navigator,
    // once the UI is open to anonymous access
    @Test
    public void theWebAgentTokenIsGivenOnlyToAnAuthenticatedCall() throws Exception {
        String agentToken = noauth("HttpClientSettings.hcsAgentToken", "");
        Assert.assertFalse(agentToken.isEmpty());

        String userToken = noauth("HttpRoleSettings.hrsUser&p=eadAgentUser", "");
        Assert.assertEquals(agentToken, navigatorAgentToken(new AuthenticationToken(userToken)));
        Assert.assertEquals(agentToken, new JSONObject(call("Service.getClientSettings[]", "", "Authorization", "Bearer " + userToken)).optString("webAgentToken"));

        Assert.assertEquals("", new JSONObject(noauth("Service.getClientSettings[]", "")).optString("webAgentToken"));
        noauth("HttpClientSettings.hcsEnableUI&p=2", "");
        try {
            Assert.assertNull(navigatorAgentToken(AuthenticationToken.ANONYMOUS));
        } finally {
            noauth("HttpClientSettings.hcsEnableUI", "");
        }
    }

    // the desktop client asks which of the logins its login dialog keeps are of users that can log in : after the login,
    // by the token of the user logged in. A call with no login is refused alike for a login that exists and for one
    // that does not
    @Test
    public void theSavedLoginsAreSyncedOnlyAfterALogin() throws Exception {
        HttpResponse<String> existing = post("Authentication.syncUsers&p=eadComputer", savedLogins("admin"));
        HttpResponse<String> missing = post("Authentication.syncUsers&p=eadComputer", savedLogins("eadNobody"));
        Assert.assertEquals(existing.body(), 401, existing.statusCode());
        Assert.assertEquals(missing.body(), 401, missing.statusCode());

        Assert.assertEquals(List.of("admin"), syncUsers(savedLogins("admin", "eadNobody")));
    }

    // as the web client reads it after the login, by a navigator of its own
    private static String navigatorAgentToken(AuthenticationToken token) throws Exception {
        NavigatorInfo info = new NavigatorInfo(new SessionInfo(new ConnectionInfo(new ComputerInfo("localhost", "127.0.0.1"), UserInfo.NULL),
                ExternalRequest.EMPTY), null, null, null, null, null, null, null, null, null,
                BaseUtils.getPlatformVersion(), BaseUtils.getApiVersion());
        RemoteNavigatorInterface navigator = RMIUtils.<RemoteLogicsLoaderInterface>rmiLookup("localhost", rmiPort, "default", "RemoteLogicsLoader")
                .getLogics().createNavigator(token, info);
        try {
            return LogicsSessionObject.getClientSettings(ExternalRequest.EMPTY, navigator, null).webAgentToken;
        } finally {
            navigator.close();
        }
    }

    // as the web client names a provider's user, and with the web client's secret
    private static OAuth2Authentication oauthLogin(String userName, String email) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("email", email);
        userInfo.put("name", "First Last");
        return new OAuth2Authentication(userName, "secretKey", userInfo);
    }

    // the first login of a provider's user refused, with no user created
    private static void assertRefused(RemoteLogicsInterface logics, OAuth2Authentication login) throws Exception {
        try {
            logics.authenticateUser(login);
            Assert.fail("the first OAuth login of " + login.getUserName() + " logged in while registration is disabled");
        } catch (LoginException e) {
            // refused
        }
        Assert.assertFalse(userExists(login.getUserName()));
    }

    // by the desktop client's call, which keeps of the logins it is given those of the users that can log in
    private static boolean userExists(String login) throws Exception {
        return !syncUsers(savedLogins(login)).isEmpty();
    }

    // the logins as the desktop client sends them
    private static String savedLogins(String... logins) {
        return new JSONArray(logins).toString();
    }

    private static String syncToken;
    // the desktop client's call after the login, by the token of a user of its own
    private static List<Object> syncUsers(String savedLogins) throws Exception {
        if (syncToken == null)
            syncToken = noauth("HttpRoleSettings.hrsUser&p=eadSyncUser", "");
        return new JSONArray(call("Authentication.syncUsers&p=eadComputer", savedLogins, "Authorization", "Bearer " + syncToken)).toList();
    }

    // an anonymous call that has to run : its answer
    private static String noauth(String action, String body) throws Exception {
        return call(action, body);
    }

    // an anonymous call the API lets in (it refuses with 401), which the action itself refuses
    private static void refusedByAction(String action, String body) throws Exception {
        HttpResponse<String> response = post(action, body);
        Assert.assertEquals(action + ": " + response.body(), 403, response.statusCode());
    }

    private static String call(String action, String body, String... headers) throws Exception {
        HttpResponse<String> response = post(action, body, headers);
        Assert.assertEquals(action + ": " + response.body(), 200, response.statusCode());
        return response.body();
    }

    // the action with its parameters after it, its signature's brackets escaped : a body that is a json array or object
    // is sent as json
    private static HttpResponse<String> post(String action, String body, String... headers) throws Exception {
        String contentType = body.startsWith("[") || body.startsWith("{") ? "application/json" : "text/plain";
        return TestServer.post(httpPort, "/exec?action=" + action.replace("[", "%5B").replace("]", "%5D"), body, contentType, headers);
    }
}
