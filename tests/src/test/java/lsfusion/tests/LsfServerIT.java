package lsfusion.tests;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static lsfusion.tests.TestServer.BASE;
import static lsfusion.tests.TestServer.read;

/** The behaviour tests. One server and one database for the whole run. Every action named test* is called through
 *  the external http /exec, which runs it in a session of its own and, since a test action does not apply, throws
 *  its changes away afterwards. /exec has no client to show a form to, and the server is started with
 *  openFormsWithoutClient, so that SHOW ... NOWAIT still opens one: the form runs its ON INIT and is closed right away.
 *  A test is named by where its module sits: forms/EvalForms.testEvalGroupObjectRef, which is what -Dlsf.tests=
 *  filters on and what quarantine.txt lists. */
@RunWith(Parameterized.class)
public class LsfServerIT {

    private static final Path LSF = BASE.resolve("src/main/lsfusion");
    private static final Pattern MODULE = Pattern.compile("^MODULE\\s+(\\w+)\\s*;", Pattern.MULTILINE);
    private static final Pattern TEST = Pattern.compile("^(test\\w*)\\b", Pattern.MULTILINE);

    private static final String DATABASE = "lsfusion_test_" + ProcessHandle.current().pid();

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private static Process server;
    private static int httpPort;

    private final String name;
    private final String canonicalName;
    private final boolean quarantined;

    public LsfServerIT(String name, String canonicalName, boolean quarantined) {
        this.name = name;
        this.canonicalName = canonicalName;
        this.quarantined = quarantined;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> tests() throws IOException {
        String filter = System.getProperty("lsf.tests");
        Pattern pattern = glob(filter != null ? filter : "*");
        Set<String> quarantine = TestServer.quarantine();

        List<Object[]> tests = new ArrayList<>();
        try (Stream<Path> modules = Files.walk(LSF)) {
            for (Path module : (Iterable<Path>) modules.filter(path -> path.toString().endsWith(".lsf")).sorted()::iterator) {
                String text = read(module);
                Matcher moduleName = MODULE.matcher(text);
                if (!moduleName.find())
                    continue;

                Path directory = LSF.relativize(module).getParent();
                Matcher action = TEST.matcher(text);
                while (action.find()) {
                    String canonicalName = moduleName.group(1) + "." + action.group(1) + "[]";
                    String name = (directory == null ? "" : directory.toString().replace('\\', '/') + "/") + moduleName.group(1) + "." + action.group(1);
                    if (pattern.matcher(name).matches())
                        tests.add(new Object[]{name, canonicalName, quarantine.contains(name)});
                }
            }
        }
        // junit reports an empty run as a passing one, which for a mistyped filter would read as the test having passed
        if (tests.isEmpty() && filter != null)
            throw new IllegalArgumentException("no test matches -Dlsf.tests=" + filter);
        return tests;
    }

    private static Pattern glob(String pattern) {
        StringBuilder regex = new StringBuilder();
        for (String part : pattern.split("\\*", -1))
            regex.append(regex.length() > 0 ? ".*" : "").append(Pattern.quote(part));
        return Pattern.compile(regex.toString());
    }

    @BeforeClass
    public static void startServer() throws Exception {
        int[] ports = TestServer.freePorts(4);
        httpPort = ports[0];
        Path log = BASE.resolve("target/server.log");
        server = TestServer.start(DATABASE, ports, log,
                "settings.enableAPI=2", // anonymous, so a test action needs no user
                "settings.openFormsWithoutClient=true"); // so that a test can SHOW a form, see above
        System.out.println("lsFusion test server: database " + DATABASE + ", http port " + httpPort + ", log " + log);
        TestServer.waitStarted(server, log);
    }

    @AfterClass
    public static void stopServer() throws Exception {
        if (server != null)
            TestServer.stop(server, DATABASE);
    }

    @Test
    public void runs() throws Exception {
        HttpResponse<String> response = HTTP.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + httpPort + "/exec?action=" + URLEncoder.encode(canonicalName, "UTF-8")))
                        .timeout(Duration.ofMinutes(10)).POST(HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());

        boolean passed = response.statusCode() == 200;
        String message = name + "\n" + response.body();
        if (quarantined)
            Assume.assumeTrue(message, passed); // still run, so it is known when it starts passing again
        else
            Assert.assertTrue(message, passed);
    }
}
