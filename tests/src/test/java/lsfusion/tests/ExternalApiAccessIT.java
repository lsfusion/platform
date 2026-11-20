package lsfusion.tests;

import lsfusion.base.remote.RMIUtils;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.connection.ComputerInfo;
import lsfusion.interop.connection.ConnectionInfo;
import lsfusion.interop.connection.UserInfo;
import lsfusion.interop.logics.remote.RemoteLogicsInterface;
import lsfusion.interop.logics.remote.RemoteLogicsLoaderInterface;
import lsfusion.interop.session.ExternalRequest;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

import static lsfusion.tests.TestServer.BASE;

/** The external API's access check, on a server of its own: the behaviour tests' server lets every request in
 *  (enableAPI=2), this one only an authenticated one (enableAPI=1), which is where an action's @@noauth matters. */
public class ExternalApiAccessIT {

    private static final String DATABASE = "lsfusion_test_api_" + ProcessHandle.current().pid();

    private static final HttpClient HTTP = HttpClient.newHttpClient();

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

    // the annotations of a script's run are written by whoever sends the script, not by the developer
    @Test
    public void aScriptsOwnNoauthDoesNotLetItRunWithoutLogin() throws Exception {
        HttpResponse<String> response = post("/eval", "run() { EXPORT JSON FROM result = 1; } @@noauth;");
        Assert.assertEquals(response.body(), 401, response.statusCode());
    }

    // what the code that customizes the application declares is opened to the callers by its own annotations, as an
    // action of the logics is : by @@noauth here, on a server that lets only an authenticated call in. A call takes the
    // code its session is built with, so the calls after the save open sessions of the stateful API of their own, of names
    // no call has used : the server drops a closed session after it has answered
    @Test
    public void aNoauthActionOfACustomizationRunsWithoutLogin() throws Exception {
        String code = "eaaOpen() { RETURN 7; } @@noauth;\neaaClosed() { RETURN 8; }";
        HttpResponse<String> set = post("/exec?action=HttpExtendCode.hecSet&p=" + URLEncoder.encode(code, "UTF-8"), "");
        Assert.assertEquals(set.body(), 200, set.statusCode());
        try {
            HttpResponse<String> open = post("/exec?action=eaaOpen&session=" + UUID.randomUUID() + "_close", "");
            Assert.assertEquals(open.body(), 200, open.statusCode());
            Assert.assertEquals("7", open.body());

            HttpResponse<String> closed = post("/exec?action=eaaClosed&session=" + UUID.randomUUID() + "_close", "");
            Assert.assertEquals(closed.body(), 401, closed.statusCode());
        } finally {
            post("/exec?action=HttpExtendCode.hecReset", "");
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

    private static HttpResponse<String> post(String path, String body) throws Exception {
        return post(path, body, "text/plain");
    }

    private static HttpResponse<String> post(String path, String body, String contentType) throws Exception {
        return post(path, body, contentType, null);
    }

    private static HttpResponse<String> post(String path, String body, String contentType, String authorization) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + httpPort + path))
                .timeout(Duration.ofMinutes(1))
                .header("Content-Type", contentType + "; charset=UTF-8");
        if (authorization != null)
            request.header("Authorization", authorization);
        return HTTP.send(request.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(), HttpResponse.BodyHandlers.ofString());
    }
}
