package lsfusion.tests;

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
import java.util.stream.Stream;

import static lsfusion.tests.TestServer.BASE;

/** The external API's access check, on a server of its own: the behaviour tests' server lets every request in
 *  (enableAPI=2), this one only an authenticated one (enableAPI=1), which is where an action's @@noauth matters. */
public class ExternalApiAccessIT {

    private static final String DATABASE = "lsfusion_test_api_" + ProcessHandle.current().pid();

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private static Process server;
    private static int httpPort;

    @BeforeClass
    public static void startServer() throws Exception {
        int[] ports = TestServer.freePorts(4);
        httpPort = ports[0];
        Path log = BASE.resolve("target/server-api.log");
        server = TestServer.start(DATABASE, ports, log, BASE.resolve("target/classes"),
                "settings.enableAPI=1",
                "lsfusion.server.devmode=false"); // which would set enableAPI to 2 whatever is given
        TestServer.waitStarted(server, log);
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
    // action of the logics is : by @@noauth here, on a server that lets only an authenticated call in
    @Test
    public void aNoauthActionOfACustomizationRunsWithoutLogin() throws Exception {
        String code = "eaaOpen() { RETURN 7; } @@noauth;\neaaClosed() { RETURN 8; }";
        HttpResponse<String> set = post("/exec?action=HttpExtendCode.hecSet&p=" + URLEncoder.encode(code, "UTF-8"), "");
        Assert.assertEquals(set.body(), 200, set.statusCode());
        try {
            HttpResponse<String> open = post("/exec?action=eaaOpen", "");
            Assert.assertEquals(open.body(), 200, open.statusCode());
            Assert.assertEquals("7", open.body());

            HttpResponse<String> closed = post("/exec?action=eaaClosed", "");
            Assert.assertEquals(closed.body(), 401, closed.statusCode());
        } finally {
            post("/exec?action=HttpExtendCode.hecReset", "");
        }
    }

    // a session of the pool reads the code on every call and serves many calls one after another : a broken code fails,
    // and is logged, once for the session, not once for every call. The calls are let apart for a session to be back in
    // the pool before the next one comes; it is returned asynchronously, so a call can still get another session - each
    // fails once, and they are far fewer than the calls
    @Test
    public void aBrokenCustomizationIsNotCompiledOnEveryCall() throws Exception {
        HttpResponse<String> set = post("/exec?action=HttpExtendCode.hecSet&p=" + URLEncoder.encode("eaaBroken( {", "UTF-8"), "");
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
        return HTTP.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + httpPort + path))
                        .timeout(Duration.ofMinutes(1))
                        .header("Content-Type", "text/plain; charset=UTF-8")
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
