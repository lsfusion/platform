package lsfusion.tests;

import lsfusion.base.remote.RMIUtils;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.connection.ComputerInfo;
import lsfusion.interop.connection.ConnectionInfo;
import lsfusion.interop.connection.UserInfo;
import lsfusion.interop.connection.authentication.PasswordAuthentication;
import lsfusion.interop.logics.remote.RemoteLogicsInterface;
import lsfusion.interop.logics.remote.RemoteLogicsLoaderInterface;
import lsfusion.interop.session.ExternalRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static lsfusion.tests.TestServer.BASE;

/** The external API's access check, on a server of its own: the behaviour tests' server lets every request in
 *  (enableAPI=2), this one only an authenticated one (enableAPI=1), which is where an action's @@noauth matters. */
public class ExternalApiAccessIT {

    private static final String DATABASE = "lsfusion_test_api_" + ProcessHandle.current().pid();
    private static final String SECOND_FACTOR_PASSWORD = "eaaPassword1";
    private static final String ADMIN_PASSWORD = "external-api-test";

    private static Process server;
    private static int httpPort;
    private static RemoteLogicsInterface logics;

    @BeforeClass
    public static void startServer() throws Exception {
        int[] ports = TestServer.freePorts(4);
        httpPort = ports[0];
        Path log = BASE.resolve("target/server-api.log");
        server = TestServer.start(DATABASE, ports, log, BASE.resolve("target/classes"),
                "settings.enableAPI=1",
                "logics.initialAdminPassword=" + ADMIN_PASSWORD,
                "lsfusion.server.devmode=false"); // which would set enableAPI to 2 whatever is given
        TestServer.waitStarted(server, log);
        logics = RMIUtils.<RemoteLogicsLoaderInterface>rmiLookup("localhost", ports[1], "default", "RemoteLogicsLoader").getLogics();
    }

    @AfterClass
    public static void stopServer() throws Exception {
        if (server != null)
            TestServer.stop(server, DATABASE);
    }

    // a @@noauth action the web client calls before its user logs in
    @Test
    public void aNoauthActionRunsWithoutLogin() throws Exception {
        HttpResponse<String> response = post("/exec?action=" + URLEncoder.encode("Service.getServerSettings[]", "UTF-8"), "");
        Assert.assertEquals(response.body(), 200, response.statusCode());
    }

    // an error is answered as text, whatever its message says
    @Test
    public void anErrorIsAnsweredAsText() throws Exception {
        String name = "eaa<b>NoAction</b>";
        HttpResponse<String> response = TestServer.get(httpPort, "/exec?action=" + URLEncoder.encode(name, "UTF-8"));
        Assert.assertEquals(response.body(), 404, response.statusCode());
        List<String> contentTypes = response.headers().allValues("Content-Type");
        Assert.assertTrue(contentTypes.toString(), contentTypes.size() == 1 && contentTypes.get(0).startsWith("text/plain;"));
        Assert.assertTrue(response.body(), response.body().contains(name));
    }

    // the annotations of a script's run are written by whoever sends the script, not by the developer
    @Test
    public void aScriptsOwnNoauthDoesNotLetItRunWithoutLogin() throws Exception {
        HttpResponse<String> response = post("/eval", "run() { EXPORT JSON FROM result = 1; } @@noauth;");
        Assert.assertEquals(response.body(), 401, response.statusCode());
    }

    // a script's action runs in a stack of its module, as EVAL runs it : an EVAL inside the script sees what the script
    // declares
    @Test
    public void anEvalInsideAScriptSeesWhatTheScriptDeclares() throws Exception {
        String authorization = "Bearer " + userToken("eaaNestedEval", null);
        HttpResponse<String> response = post("/eval", "eaaOuter = DATA LOCAL INTEGER ();\nrun() { EVAL 'run() \\{ eaaOuter() <- 7; \\}'; RETURN eaaOuter(); }", "text/plain", authorization);
        Assert.assertEquals(response.body(), 200, response.statusCode());
        Assert.assertEquals("7", response.body());
    }

    // a code of every kind is saved only by a call that may evaluate a script : an authenticated one, not one with no login
    @Test
    public void writingCustomizationCodeRequiresTheRightToEvaluate() throws Exception {
        for (String kind : new String[]{"connection", "form", "ownConnection", "ownForm"}) {
            String write = "/exec?action=ExtendCodeRights.ecrWrite&p=" + kind;
            Assert.assertEquals(kind, "refused", post(write, "").body());
            Assert.assertEquals(kind, "saved", postAsAdmin(write, "").body());
        }
    }

    // the code of the module LogicsExtendCode is saved, and dropped, only by a call that may evaluate a script : a drop
    // changes the logics as a code does
    @Test
    public void theLogicsCodeRequiresTheRightToEvaluate() throws Exception {
        Assert.assertEquals("refused", post("/exec?action=ExtendCodeRights.ecrSaveLogics", "").body());
        Assert.assertEquals("saved", postAsAdmin("/exec?action=ExtendCodeRights.ecrSaveLogics", "").body());
        Assert.assertEquals("refused", post("/exec?action=ExtendCodeRights.ecrDropLogics", "").body());
        Assert.assertEquals("dropped", postAsAdmin("/exec?action=ExtendCodeRights.ecrDropLogics", "").body());
    }

    // a form is built of the code saved, not of one its session wrote and did not save, for any call
    @Test
    public void aFormIsBuiltOfTheSavedCode() throws Exception {
        HttpResponse<String> save = postAsAdmin("/exec?action=ExtendCodeRights.ecrSaveForm", "");
        Assert.assertEquals(save.body(), 200, save.statusCode());
        try {
            Assert.assertEquals("saved", post("/exec?action=ExtendCodeRights.ecrBuild", "").body());
            Assert.assertEquals("saved", postAsAdmin("/exec?action=ExtendCodeRights.ecrBuild", "").body());
        } finally {
            postAsAdmin("/exec?action=ExtendCodeRights.ecrDropForm", "");
        }
    }

    // what the code that customizes the application declares is opened to the callers by its own annotations, as an
    // action of the logics is : by @@noauth here, on a server that lets only an authenticated call in. The calls after the
    // save open sessions of the stateful API of their own, of names no call has used : the server drops a closed session
    // after it has answered
    @Test
    public void aNoauthActionOfACustomizationRunsWithoutLogin() throws Exception {
        String code = "eaaOpen() { RETURN 7; } @@noauth;\neaaClosed() { RETURN 8; }";
        HttpResponse<String> set = postAsAdmin("/exec?action=HttpExtendCode.hecSet&p=" + URLEncoder.encode(code, "UTF-8"), "");
        Assert.assertEquals(set.body(), 200, set.statusCode());
        try {
            HttpResponse<String> open = post("/exec?action=eaaOpen&session=" + UUID.randomUUID() + "_close", "");
            Assert.assertEquals(open.body(), 200, open.statusCode());
            Assert.assertEquals("7", open.body());

            HttpResponse<String> closed = post("/exec?action=eaaClosed&session=" + UUID.randomUUID() + "_close", "");
            Assert.assertEquals(closed.body(), 401, closed.statusCode());
        } finally {
            postAsAdmin("/exec?action=HttpExtendCode.hecReset", "");
        }
    }

    // registerUser is open to an anonymous call, as the registration page calls it : registration is switched off on a
    // new database, for the call too, and the user is not created - until an administrator switches it on
    @Test
    public void aNewDatabaseRegistersNoOneByTheApi() throws Exception {
        String user = "[{\"login\":\"eaaRegistered\",\"password\":\"eaaPassword1\",\"firstName\":\"First\",\"lastName\":\"Last\",\"email\":\"eaa@example.com\"}]";

        HttpResponse<String> refused = post("/exec?action=Authentication.registerUser", user, "application/json");
        Assert.assertEquals(refused.body(), 200, refused.statusCode());
        JSONObject refusal = new JSONObject(refused.body());
        Assert.assertTrue(refused.body(), refusal.has("error") && !refusal.has("success"));

        HttpResponse<String> enable = post("/exec?action=HttpRegistration.hrEnable", "");
        Assert.assertEquals(enable.body(), 200, enable.statusCode());
        try {
            // the login is still free
            HttpResponse<String> registered = post("/exec?action=Authentication.registerUser", user, "application/json");
            Assert.assertEquals(registered.body(), 200, registered.statusCode());
            JSONObject answer = new JSONObject(registered.body());
            Assert.assertTrue(registered.body(), answer.has("success") && !answer.has("error"));
        } finally {
            post("/exec?action=HttpRegistration.hrDisable", "");
        }
    }

    // a script sent to run is logged with who sent it before it is compiled : one that is refused is there too
    @Test
    public void aRefusedScriptIsLoggedWithWhoSentIt() throws Exception {
        String marker = "eaaLogged" + UUID.randomUUID().toString().replace("-", "");
        HttpResponse<String> refused = post("/eval", "run() { MESSAGE '" + marker + "'; }");
        Assert.assertEquals(refused.body(), 401, refused.statusCode());

        String entry = evalLogEntry(marker, 0);
        Assert.assertTrue(entry, entry.startsWith("EVAL - User : anonymous") && entry.contains("Remote : ") && entry.endsWith(":\n\trun() { MESSAGE '"));
    }

    // the script the EVAL operator runs is logged too, each of its lines indented, so that none reads as an entry - and
    // each time it is run, though it is compiled only the first time
    @Test
    public void aScriptOfTheEvalOperatorIsLoggedEachTimeItIsRun() throws Exception {
        String script = ":\n\trun() {\n\t    // elOperatorLogged\n\t}";
        int from = evalLog().length();
        for (int time = 0; time < 2; time++) {
            HttpResponse<String> run = post("/exec?action=EvalLog.elRun", "");
            Assert.assertEquals(run.body(), 200, run.statusCode());

            String entry = evalLogEntry(script, from);
            Assert.assertTrue(entry, entry.startsWith("EVAL - "));
            from = evalLog().indexOf(script, from) + script.length();
        }
    }

    // a role's own enableAPI decides for its users, where it closes the API the server opens and where it opens the API
    // the server closes : on every way into the API - an action run by its name, a script, an action's body, an MCP
    // tool. The users' calls take turns : a call is made by a session of its own or by one the API's pool keeps after a
    // call of the other user, whichever the pool has back by then
    @Test
    public void aRolesEnableAPIDecidesForItsUsers() throws Exception {
        String closed = userToken("eaaRoleClosed", "0");
        String own = userToken("eaaRoleOwn", null);
        for (int round = 0; round < 3; round++) {
            assertLetIn(own);
            assertDisabled(closed);
        }

        try {
            Assert.assertEquals(200, post("/exec?action=HttpRoleSettings.hrsServer&p=0", "").statusCode());
            String open = userToken("eaaRoleOpen", "1");
            for (int round = 0; round < 3; round++) {
                assertLetIn(open);
                assertDisabled(own);
            }
        } finally {
            Assert.assertEquals(200, post("/exec?action=HttpRoleSettings.hrsServer", "").statusCode());
        }
    }

    // at enableAPI=0 the API is open to a user who can run code in the interpreter, as that user can run any code there
    // anyway - and only to such a user : one who may open the interpreter but may change nothing or may not see its run,
    // so cannot run it, or who has no UI at all, gets no API by it
    @Test
    public void theInterpreterOpensTheAPIOnlyToAUserWhoCanRunIt() throws Exception {
        String runs = interpreterUserToken("eaaInterpreterRuns", null);
        String readOnly = interpreterUserToken("eaaInterpreterReadOnly", "readonly");
        String noView = interpreterUserToken("eaaInterpreterNoView", "noview");
        String noUI = interpreterUserToken("eaaInterpreterNoUI", "noui");
        try {
            Assert.assertEquals(200, post("/exec?action=HttpRoleSettings.hrsServer&p=0", "").statusCode());
            assertLetIn(runs);
            assertDisabled(readOnly);
            assertDisabled(noView);
            assertDisabled(noUI);
        } finally {
            Assert.assertEquals(200, post("/exec?action=HttpRoleSettings.hrsServer", "").statusCode());
        }
    }

    // a disabled role gives its users none of its working parameters, as it gives them none of its permissions : they
    // get the ones of their next role - an enableAPI of the disabled role opens no API the server closes, and closes
    // none the next role opens
    @Test
    public void aDisabledRolesEnableAPIDoesNotApply() throws Exception {
        try {
            Assert.assertEquals(200, post("/exec?action=HttpRoleSettings.hrsServer&p=0", "").statusCode());
            assertDisabled(disabledFirstRoleUserToken("eaaDisabledOpens", "1", null));
            assertLetIn(disabledFirstRoleUserToken("eaaDisabledCloses", "0", "1"));
        } finally {
            Assert.assertEquals(200, post("/exec?action=HttpRoleSettings.hrsServer", "").statusCode());
        }
    }

    // the same for the default role a user without an enabled role of its own falls back to : disabled, it gives no
    // settings, and the server's apply
    @Test
    public void aDisabledDefaultRoleGivesNoSettings() throws Exception {
        HttpResponse<String> roles = post("/exec?action=HttpRoleSettings.hrsFallbackRoles", "");
        Assert.assertEquals(roles.body(), 200, roles.statusCode());
        Assert.assertEquals("default NULL", roles.body());
    }

    private static String disabledFirstRoleUserToken(String login, String first, String second) throws Exception {
        HttpResponse<String> user = post("/exec?action=HttpRoleSettings.hrsDisabledFirstRoleUser&p=" + login + "&p=" + first + (second != null ? "&p=" + second : ""), "");
        Assert.assertEquals(user.body(), 200, user.statusCode());
        return user.body();
    }

    private static String interpreterUserToken(String login, String kind) throws Exception {
        HttpResponse<String> user = post("/exec?action=HttpRoleSettings.hrsInterpreterUser&p=" + login + (kind != null ? "&p=" + kind : ""), "");
        Assert.assertEquals(user.body(), 200, user.statusCode());
        return user.body();
    }

    private static String userToken(String login, String enableAPI) throws Exception {
        HttpResponse<String> user = post("/exec?action=HttpRoleSettings.hrsUser&p=" + login + (enableAPI != null ? "&p=" + enableAPI : ""), "");
        Assert.assertEquals(user.body(), 200, user.statusCode());
        return user.body();
    }

    private static void assertLetIn(String token) throws Exception {
        for (HttpResponse<String> response : apiCalls(token))
            Assert.assertEquals(response.uri() + " answered " + response.body(), "pong", response.statusCode() == 200 ? response.body() : null);
        JSONObject tool = mcpEval(token);
        Assert.assertFalse(tool.toString(), tool.optBoolean("isError"));
        Assert.assertEquals("pong", tool.getJSONObject("structuredContent").getJSONArray("results").getJSONObject(0).getString("value"));
    }

    private static void assertDisabled(String token) throws Exception {
        for (HttpResponse<String> response : apiCalls(token))
            Assert.assertTrue(response.uri() + " answered " + response.statusCode() + " " + response.body(), response.statusCode() == 500 && response.body().contains("Api is disabled"));
        JSONObject tool = mcpEval(token);
        Assert.assertTrue(tool.toString(), tool.optBoolean("isError") && tool.toString().contains("Api is disabled"));
    }

    private static HttpResponse<String>[] apiCalls(String token) throws Exception {
        String authorization = "Bearer " + token;
        return new HttpResponse[]{
                post("/exec?action=HttpRoleSettings.hrsPing", "", "text/plain", authorization),
                post("/eval", "run() { RETURN 'pong'; }", "text/plain", authorization),
                post("/eval/action", "RETURN 'pong';", "text/plain", authorization)};
    }

    private static JSONObject mcpEval(String token) throws Exception {
        String request = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"lsfusion_eval\",\"arguments\":"
                + "{\"script\":" + JSONObject.quote("run() { RETURN 'pong'; }") + ",\"params\":[]}}}";
        JSONObject response = new JSONObject(logics.mcp(new AuthenticationToken(token), new ConnectionInfo(new ComputerInfo("localhost", "127.0.0.1"), UserInfo.NULL),
                new ExternalRequest(new ExternalRequest.Param[0], null, null, null, null, null, null, null, null, null, null, null, null, null, null), request).json);
        Assert.assertTrue(response.toString(), response.has("result"));
        return response.getJSONObject("result");
    }

    // the server gives the web client the providers it offers for login : none while none is configured, then the
    // configured one, with its client secret
    @Test
    public void theServerGivesTheLoginProviders() throws Exception {
        Assert.assertNull(logics.getClientCredentials());

        post("/exec?action=HttpWebClientLogin.hwlConfigure", "");
        try {
            JSONArray providers = new JSONArray(logics.getClientCredentials());
            Assert.assertEquals(providers.toString(), 1, providers.length());
            JSONObject provider = providers.getJSONObject(0);
            Assert.assertEquals("github", provider.getString("id"));
            Assert.assertEquals("hwlClientId", provider.getString("clientId"));
            Assert.assertEquals("hwlClientSecret", provider.getString("clientSecret"));
            Assert.assertFalse(provider.toString(), provider.has("jwkSetUri"));
        } finally {
            post("/exec?action=HttpWebClientLogin.hwlReset", "");
        }
    }

    // a user who logs in with the password and a code from the mail: the password alone, in the API's Basic login, runs
    // nothing as the user
    @Test
    public void aPasswordAloneRunsNothingAsAUserWithTheSecondFactor() throws Exception {
        String login = secondFactorUser("eaaSecondFactorBasic");
        try {
            String basic = "Basic " + Base64.getEncoder().encodeToString((login + ":" + SECOND_FACTOR_PASSWORD).getBytes(StandardCharsets.UTF_8));
            HttpResponse<String> who = post("/exec?action=HttpSecondFactor.hsfWho", "", "text/plain", basic);
            Assert.assertEquals(who.body(), 401, who.statusCode());
        } finally {
            secondFactorOff();
        }
    }

    // the code is checked by the token the password login gave: a wrong code counts, the last allowed one drops the
    // code, the right code gives a token the calls run as the user with
    @Test
    public void theCodeIsCheckedForTheUserOfThePasswordsToken() throws Exception {
        String login = secondFactorUser("eaaSecondFactorCode");
        try {
            String token = logics.authenticateUser(new PasswordAuthentication(login, SECOND_FACTOR_PASSWORD)).string;

            setCode(login, "123456");
            Assert.assertEquals("invalid", verify(token, "654321").optString("error"));
            Assert.assertEquals("invalid", verify(token, "654321").optString("error"));
            Assert.assertEquals("attempts", verify(token, "654321").optString("error"));
            Assert.assertEquals("expired", verify(token, "123456").optString("error"));

            setCode(login, "123456");
            JSONObject passed = verify(token, "123456");
            Assert.assertEquals(passed.toString(), "ok", passed.optString("success"));

            HttpResponse<String> who = post("/exec?action=HttpSecondFactor.hsfWho", "", "text/plain", "Bearer " + passed.getString("token"));
            Assert.assertEquals(who.body(), 200, who.statusCode());
            Assert.assertEquals(login, who.body());
        } finally {
            secondFactorOff();
        }
    }

    // a code is sent for the user of the token the password login gave
    @Test
    public void theCodeIsSentForTheUserOfThePasswordsToken() throws Exception {
        String login = secondFactorUser("eaaSecondFactorSend");
        try {
            String token = logics.authenticateUser(new PasswordAuthentication(login, SECOND_FACTOR_PASSWORD)).string;
            post(json2FA("send2FACode"), "[{\"token\":" + JSONObject.quote(token) + "}]", "application/json");
            Assert.assertNotEquals("none", code(login));
        } finally {
            secondFactorOff();
        }
    }

    // a login of its own for each test, the second factor switched on for the users with an e-mail. The login reads the
    // setting through a cache the server flushes every second: the test goes on once the password asks for the code
    private static String secondFactorUser(String prefix) throws Exception {
        String login = prefix + System.nanoTime();
        Assert.assertEquals(200, post("/exec?action=HttpSecondFactor.hsfOn", "").statusCode());
        HttpResponse<String> user = post("/exec?action=HttpSecondFactor.hsfUser&p=" + login + "&p=" + SECOND_FACTOR_PASSWORD, "");
        Assert.assertEquals(user.body(), 200, user.statusCode());
        long deadline = System.currentTimeMillis() + 10_000;
        while (!logics.authenticateUser(new PasswordAuthentication(login, SECOND_FACTOR_PASSWORD)).use2FA) {
            if (System.currentTimeMillis() > deadline)
                throw new AssertionError("the password of " + login + " does not ask for the code");
            Thread.sleep(200);
        }
        return login;
    }

    private static void secondFactorOff() throws Exception {
        Assert.assertEquals(200, post("/exec?action=HttpSecondFactor.hsfOff", "").statusCode());
    }

    private static void setCode(String login, String code) throws Exception {
        Assert.assertEquals(200, post("/exec?action=HttpSecondFactor.hsfSetCode&p=" + login + "&p=" + code, "").statusCode());
    }

    private static String code(String login) throws Exception {
        HttpResponse<String> code = post("/exec?action=HttpSecondFactor.hsfCode&p=" + login, "");
        Assert.assertEquals(code.body(), 200, code.statusCode());
        return code.body();
    }

    private static String json2FA(String action) throws Exception {
        return "/exec?action=" + URLEncoder.encode("Authentication." + action + "[JSONFILE]", "UTF-8");
    }

    private static JSONObject verify(String token, String code) throws Exception {
        HttpResponse<String> answer = post(json2FA("verify2FACode"), "[{\"token\":" + JSONObject.quote(token) + ",\"code\":" + JSONObject.quote(code) + "}]", "application/json");
        Assert.assertEquals(answer.body(), 200, answer.statusCode());
        return new JSONObject(answer.body());
    }

    // a messenger calls the webhook of an account with the secret it was given with the address - Telegram in a header,
    // the others in the address : a call without that secret - none, another one, the one of an account of another
    // messenger, Telegram's in the address - is answered 403 and changes nothing
    @Test
    public void aTelegramWebhookCallIsTakenOnlyWithTheSecretOfItsAddress() throws Exception {
        JSONObject accounts = messengerAccounts();
        String webhook = "/exec/webHookTelegram?botid=" + accounts.getLong("telegram");
        String header = "X-Telegram-Bot-Api-Secret-Token";
        String update = "{\"update_id\":1,\"message\":{\"message_id\":1,\"from\":{\"id\":101,\"first_name\":\"Hm\",\"username\":\"hm\"}," +
                "\"chat\":{\"id\":101,\"first_name\":\"Hm\",\"username\":\"hm\",\"type\":\"private\"},\"date\":0,\"text\":\"hm\"}}";

        assertWebhookStatus(webhook, update, 403);
        assertWebhookStatus(webhook + "&secret=hmTelegramSecret", update, 403);
        assertWebhookStatus(webhook, update, 403, header, "hmWrongSecret");
        assertWebhookStatus("/exec/webHookTelegram?botid=" + accounts.getLong("viber"), update, 403, header, "hmViberSecret");
        assertChat("hmTelegram", "101", 0, 0);
        assertChat("hmViber", "101", 0, 0);

        assertWebhookStatus(webhook, update, 200, header, "hmTelegramSecret");
        assertChat("hmTelegram", "101", 1, 1);
    }

    @Test
    public void aViberWebhookCallIsTakenOnlyWithTheSecretOfItsAddress() throws Exception {
        String webhook = "/exec/webHookViber?botid=" + messengerAccounts().getLong("viber");
        String message = "{\"event\":\"message\",\"sender\":{\"id\":\"hmv\",\"name\":\"Hm\"},\"message\":{\"text\":\"hm\"}}";
        String unsubscribed = "{\"event\":\"unsubscribed\",\"user_id\":\"hmv\"}";

        // the check of the address that Viber makes while registering it, before the secret of that address is saved
        assertWebhookStatus(webhook + "&secret=hmNotSavedSecret", "{\"event\":\"webhook\",\"timestamp\":1,\"message_token\":1}", 200);

        assertWebhookStatus(webhook, message, 403);
        assertChat("hmViber", "hmv", 0, 0);
        assertWebhookStatus(webhook + "&secret=hmViberSecret", message, 200);
        assertChat("hmViber", "hmv", 1, 1);

        assertWebhookStatus(webhook, unsubscribed, 403);
        assertWebhookStatus(webhook + "&secret=hmWrongSecret", unsubscribed, 403);
        assertChat("hmViber", "hmv", 1, 1);
        assertWebhookStatus(webhook + "&secret=hmViberSecret", unsubscribed, 200);
        assertChat("hmViber", "hmv", 0, 0);
    }

    @Test
    public void aSkypeWebhookCallIsTakenOnlyWithTheSecretOfItsAddress() throws Exception {
        String webhook = "/exec/webHookSkype?botid=" + messengerAccounts().getLong("skype");
        String add = "{\"action\":\"add\",\"serviceUrl\":\"https://hm.invalid/\",\"from\":{\"name\":\"Hm\"},\"conversation\":{\"id\":\"hms\"},\"text\":\"hm\"}";
        String remove = "{\"action\":\"remove\",\"conversation\":{\"id\":\"hms\"}}";

        assertWebhookStatus(webhook, add, 403);
        assertChat("hmSkype", "hms", 0, 0);
        assertWebhookStatus(webhook + "&secret=hmSkypeSecret", add, 200);
        assertChat("hmSkype", "hms", 1, 1);

        assertWebhookStatus(webhook, remove, 403);
        assertChat("hmSkype", "hms", 1, 1);
        assertWebhookStatus(webhook + "&secret=hmSkypeSecret", remove, 200);
        assertChat("hmSkype", "hms", 0, 0);
    }

    // Whatsapp calls one address for the application, which carries the one secret of the application
    @Test
    public void aWhatsappWebhookCallIsTakenOnlyWithTheSecretOfItsAddress() throws Exception {
        messengerAccounts();
        String webhook = "/exec/webHookWhatsapp";
        String notification = "{\"object\":\"whatsapp_business_account\",\"entry\":[{\"id\":\"1\",\"changes\":[{\"field\":\"messages\",\"value\":{" +
                "\"messaging_product\":\"whatsapp\",\"metadata\":{\"display_phone_number\":\"1\",\"phone_number_id\":\"hmPhone\"}," +
                "\"contacts\":[{\"profile\":{\"name\":\"Hm\"},\"wa_id\":\"hmw\"}]," +
                "\"messages\":[{\"from\":\"hmw\",\"id\":\"1\",\"timestamp\":\"0\",\"type\":\"text\",\"text\":{\"body\":\"hm\"}}]}}]}]}";

        assertWebhookStatus(webhook, notification, 403);
        assertWebhookStatus(webhook + "?secret=hmWrongSecret", notification, 403);
        assertChat("hmWhatsapp", "hmw", 0, 0);
        assertWebhookStatus(webhook + "?secret=hmWhatsappSecret", notification, 200);
        assertChat("hmWhatsapp", "hmw", 1, 1);

        // the check of the address, which comes with no body
        String verification = "&hub.mode=subscribe&hub.verify_token=hmVerify&hub.challenge=4242";
        HttpResponse<String> refused = TestServer.get(httpPort, webhook + "?secret=hmWrongSecret" + verification);
        Assert.assertEquals(refused.body(), 403, refused.statusCode());
        HttpResponse<String> verified = TestServer.get(httpPort, webhook + "?secret=hmWhatsappSecret" + verification);
        Assert.assertEquals(verified.body(), 200, verified.statusCode());
        Assert.assertTrue(verified.body(), verified.body().contains("4242"));
    }

    private static JSONObject messengerAccounts() throws Exception {
        HttpResponse<String> setUp = post("/exec?action=HttpMessengers.hmSetUp", "");
        Assert.assertEquals(setUp.body(), 200, setUp.statusCode());
        return new JSONObject(setUp.body());
    }

    private static void assertWebhookStatus(String path, String body, int status, String... headers) throws Exception {
        HttpResponse<String> response = TestServer.post(httpPort, path, body, "application/json", headers);
        Assert.assertEquals(path + "\n" + response.body(), status, response.statusCode());
    }

    private static void assertChat(String account, String chat, int chats, int messages) throws Exception {
        HttpResponse<String> response = post("/exec?action=HttpMessengers.hmChat&p=" + account + "&p=" + chat, "");
        Assert.assertEquals(response.body(), 200, response.statusCode());
        JSONObject counts = new JSONObject(response.body());
        Assert.assertEquals(account + " " + chat + " " + counts, chats, counts.optInt("chats"));
        Assert.assertEquals(account + " " + chat + " " + counts, messages, counts.optInt("messages"));
    }

    // the server writes its logs into the folder it is started in
    private static String evalLog() throws Exception {
        Path log = BASE.resolve("logs/eval.log");
        return Files.exists(log) ? Files.readString(log, StandardCharsets.UTF_8) : "";
    }

    // the entry of the eval log, written after from, that holds the text : from its logger's name to the text
    private static String evalLogEntry(String text, int from) throws Exception {
        long deadline = System.currentTimeMillis() + 10_000;
        while (true) {
            String log = evalLog();
            int at = log.indexOf(text, from);
            if (at >= 0) {
                String logger = "EvalLogger - ";
                return log.substring(log.lastIndexOf(logger, at) + logger.length(), at);
            }
            if (System.currentTimeMillis() > deadline)
                throw new AssertionError("no entry of " + text + " in the eval log");
            Thread.sleep(200);
        }
    }

    // a session of the pool builds its context, the code with it, once and serves many calls one after another : a broken
    // code fails, and is logged, once for the session, not once for every call. The calls are let apart for a session to be
    // back in the pool before the next one comes; it is returned asynchronously, so a call can still get another session -
    // each fails once, and they are far fewer than the calls. The calls are anonymous, made with no token
    @Test
    public void aBrokenCustomizationIsNotCompiledOnEveryCall() throws Exception {
        HttpResponse<String> set = post("/exec?action=HttpExtendCode.hecSetBroken&p=" + URLEncoder.encode("eaaBroken( {", "UTF-8"), "");
        Assert.assertEquals(set.body(), 200, set.statusCode());
        try {
            int calls = 20;
            long before = brokenCodeLogged();
            for (int i = 0; i < calls; i++) {
                Thread.sleep(200);
                HttpResponse<String> call = post("/exec?action=HttpExtendCode.hecNothing", "");
                Assert.assertEquals(call.body(), 200, call.statusCode());
            }
            long failed = brokenCodeLogged() - before;
            Assert.assertTrue("the broken code failed " + failed + " times in " + calls + " calls", failed > 0 && failed < calls / 2);
        } finally {
            post("/exec?action=HttpExtendCode.hecReset", "");
        }
    }

    private static long brokenCodeLogged() throws Exception {
        try (Stream<String> lines = Files.lines(BASE.resolve("target/server-api.log"), StandardCharsets.UTF_8)) {
            return lines.filter(line -> line.contains("Application customization code is broken")).count();
        }
    }

    private static HttpResponse<String> post(String path, String body) throws Exception {
        return post(path, body, "text/plain");
    }

    private static HttpResponse<String> postAsAdmin(String path, String body) throws Exception {
        String credentials = Base64.getEncoder().encodeToString(("admin:" + ADMIN_PASSWORD).getBytes(StandardCharsets.UTF_8));
        return post(path, body, "text/plain", "Basic " + credentials);
    }

    private static HttpResponse<String> post(String path, String body, String contentType) throws Exception {
        return post(path, body, contentType, null);
    }

    private static HttpResponse<String> post(String path, String body, String contentType, String authorization) throws Exception {
        String[] headers = authorization != null ? new String[] {"Authorization", authorization} : new String[0];
        return TestServer.post(httpPort, path, body, contentType, headers);
    }
}
