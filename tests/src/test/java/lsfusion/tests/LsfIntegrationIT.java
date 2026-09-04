package lsfusion.tests;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static lsfusion.tests.TestServer.BASE;
import static lsfusion.tests.TestServer.read;
import static lsfusion.tests.TestServer.tail;

/** Level 3, behaviour. One server and one database for the whole run. Every action named test* is called through
 *  the external http /exec, which runs it in a session of its own and, since a test action does not apply, throws
 *  its changes away afterwards. A test is named by where its module sits: forms/EvalForms.testEvalGroupObjectRef,
 *  which is what -Dlsf.tests= filters on and what quarantine.txt lists. */
@RunWith(Parameterized.class)
public class LsfIntegrationIT {

    private static final Path LSF = BASE.resolve("src/main/lsfusion");
    private static final Pattern MODULE = Pattern.compile("^MODULE\\s+(\\w+)\\s*;", Pattern.MULTILINE);
    private static final Pattern TEST = Pattern.compile("^(test\\w*)\\b", Pattern.MULTILINE);

    private static final String DATABASE = "lsfusion_test_" + System.getProperty("lsf.dbSuffix", String.valueOf(ProcessHandle.current().pid()));
    private static final String DB_SERVER = System.getProperty("db.server", "localhost");
    private static final String DB_USER = System.getProperty("db.user", "postgres");
    private static final String DB_PASSWORD = System.getProperty("db.password", "");

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private static Process server;
    private static int httpPort;

    private final String name;
    private final String canonicalName;
    private final boolean quarantined;

    public LsfIntegrationIT(String name, String canonicalName, boolean quarantined) {
        this.name = name;
        this.canonicalName = canonicalName;
        this.quarantined = quarantined;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> tests() throws IOException {
        Pattern filter = glob(System.getProperty("lsf.tests", "*"));
        Set<String> quarantine = quarantine();

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
                    if (filter.matcher(name).matches())
                        tests.add(new Object[]{name, canonicalName, quarantine.contains(name)});
                }
            }
        }
        return tests;
    }

    private static Set<String> quarantine() throws IOException {
        Set<String> quarantined = new HashSet<>();
        for (String line : Files.readAllLines(BASE.resolve("quarantine.txt")))
            if (!line.trim().isEmpty() && !line.trim().startsWith("#"))
                quarantined.add(line.trim());
        return quarantined;
    }

    private static Pattern glob(String pattern) {
        StringBuilder regex = new StringBuilder();
        for (String part : pattern.split("\\*", -1))
            regex.append(regex.length() > 0 ? ".*" : "").append(Pattern.quote(part));
        return Pattern.compile(regex.toString());
    }

    @BeforeClass
    public static void startServer() throws Exception {
        httpPort = freePort(); // not the default 7651 : whoever runs this usually has a server of their own up
        Path log = BASE.resolve("target/server.log");
        server = TestServer.start(TestServer.classPath("target/classes"), log,
                "db.name=" + DATABASE, "db.server=" + DB_SERVER, "db.user=" + DB_USER, "db.password=" + DB_PASSWORD,
                "http.port=" + httpPort, "rmi.port=" + freePort(), "webSocket.port=" + freePort(), "debugger.port=" + freePort(),
                "settings.enableAPI=2"); // anonymous, so a test action needs no user
        System.out.println("lsFusion test server: database " + DATABASE + ", http port " + httpPort + ", log " + log);

        long deadline = System.currentTimeMillis() + Duration.ofMinutes(10).toMillis();
        while (!read(log).contains("Server has successfully started")) {
            if (!server.isAlive() || System.currentTimeMillis() > deadline)
                throw new IllegalStateException("the test server did not start\n" + tail(read(log)));
            Thread.sleep(1000);
        }
    }

    @AfterClass
    public static void stopServer() throws Exception {
        if (server == null)
            return;
        server.destroy();
        server.waitFor();

        if (Boolean.getBoolean("lsf.keepDb")) {
            System.out.println("kept the test database " + DATABASE);
            return;
        }
        try (Connection connection = DriverManager.getConnection("jdbc:postgresql://" + DB_SERVER + "/postgres", DB_USER, DB_PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS " + DATABASE + " WITH (FORCE)");
        }
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
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
