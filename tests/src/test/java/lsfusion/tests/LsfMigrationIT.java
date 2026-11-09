package lsfusion.tests;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;

import static lsfusion.tests.TestServer.BASE;

/** The migration file, which a start of the server applies to the database an earlier start left. Each test starts the
 *  server several times on a database of its own, each time with one of the logics in tests/migration: v1 before the
 *  renames, v2 after them, with the migration.script that names them. That takes a few minutes, so the tests run only
 *  with -Dlsf.migrationTests=true. An action is called through the external http /exec, as in the behaviour tests. */
public class LsfMigrationIT {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @BeforeClass
    public static void enabled() {
        Assume.assumeTrue("the migration tests run with -Dlsf.migrationTests=true", Boolean.getBoolean("lsf.migrationTests"));
    }

    // #1975: a light start wrote the migration version without renaming the properties on forms and the navigator
    // elements, so the next start no longer saw these renames and dropped the old records along with their settings
    @Test
    public void lightStartKeepsRenames() throws Exception {
        String database = "lsfusion_test_" + ProcessHandle.current().pid() + "_light";
        try {
            run(database, "v1", false, "migrationSeed");
            run(database, "v2", true, "migrationCheck");
            run(database, "v2", false, "migrationCheck");
        } finally {
            TestServer.dropDatabase(database);
        }
    }

    // #1975: a full start dropped the record of a renamed form before the properties on it were looked up by its old name
    @Test
    public void fullStartKeepsRenames() throws Exception {
        String database = "lsfusion_test_" + ProcessHandle.current().pid() + "_full";
        try {
            run(database, "v1", false, "migrationSeed");
            run(database, "v2", false, "migrationCheck");
        } finally {
            TestServer.dropDatabase(database);
        }
    }

    // starts the server with the logics, calls the action and stops the server, leaving the database to the next start
    private static void run(String database, String logics, boolean lightStart, String action) throws Exception {
        String start = (lightStart ? "light" : "full") + " start with " + logics;
        Path log = BASE.resolve("target/migration/" + database + "-" + logics + "-" + (lightStart ? "light" : "full") + ".log");
        int[] ports = TestServer.freePorts(4);
        Process server = TestServer.start(database, ports, log, Arrays.asList(BASE.resolve("target/classes"), BASE.resolve("migration/" + logics)),
                "logics.topModule=MigrationRename", // target/classes is there for TestUtils, and holds every other test module too
                "settings.enableAPI=2", // anonymous, so that the action needs no user
                "lsfusion.server.lightstart=" + lightStart);
        try {
            TestServer.waitStarted(server, log);
            HttpResponse<String> response = HTTP.send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + ports[0] + "/exec?action=" + URLEncoder.encode("MigrationRename." + action + "[]", "UTF-8")))
                            .timeout(Duration.ofMinutes(10)).POST(HttpRequest.BodyPublishers.noBody()).build(),
                    HttpResponse.BodyHandlers.ofString());
            Assert.assertEquals(action + " after the " + start + "\n" + response.body(), 200, response.statusCode());
        } finally {
            TestServer.kill(server);
        }
    }
}
