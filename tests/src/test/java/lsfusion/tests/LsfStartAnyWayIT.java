package lsfusion.tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static lsfusion.tests.TestServer.BASE;
import static lsfusion.tests.TestServer.excerpt;
import static lsfusion.tests.TestServer.read;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Modules that compile but have to stop the start of the server even with startServerAnyWay, which lets it go past
 *  the errors of the database it can: what stops it then is the platform refusing the logic, where dryRun never gets.
 *  Each is started on a new database of its own and has to stop with the lines its .expected lists. */
@RunWith(Parameterized.class)
public class LsfStartAnyWayIT {

    private static final String STARTED = "Server has successfully started";

    private final String name;
    private final List<String> expected;

    public LsfStartAnyWayIT(String name, List<String> expected) {
        this.name = name;
        this.expected = expected;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> corpus() throws IOException {
        List<Object[]> cases = new ArrayList<>();
        Path fail = BASE.resolve("startanyway/fail");
        try (Stream<Path> modules = Files.walk(fail)) {
            Iterable<Path> lsfFiles = modules.filter(path -> path.toString().endsWith(".lsf")).sorted()::iterator;
            for (Path module : lsfFiles) {
                String file = fail.relativize(module).toString().replace('\\', '/');
                String name = file.substring(0, file.length() - ".lsf".length());
                List<String> expected = new ArrayList<>();
                for (String line : Files.readAllLines(fail.resolve(name + ".expected")))
                    if (!line.trim().isEmpty())
                        expected.add(line.trim());
                cases.add(new Object[]{name, expected});
            }
        }
        return cases;
    }

    @Test
    public void stops() throws Exception {
        String database = "lsfusion_test_startanyway_" + ProcessHandle.current().pid() + "_" +
                name.replaceAll("\\W", "_").toLowerCase();
        Path log = BASE.resolve("target/startanyway-" + name.replace('/', '-') + ".log");
        Process server = TestServer.start(database, TestServer.freePorts(4), log, BASE.resolve("startanyway"),
                "logics.includePaths=/fail/" + name + ".lsf", "settings.startServerAnyWay=true");
        try {
            // a server that could not start stops with status 0 as well, so it is told apart by its log
            TestServer.waitFor(() -> !server.isAlive() || read(log).contains(STARTED), server, log,
                    "the server neither started nor stopped");
            String output = read(log);
            assertFalse("started, but was expected to stop\n" + excerpt(output), output.contains(STARTED));
            for (String line : expected)
                assertTrue("not among the errors reported: " + line + "\n" + excerpt(output), output.contains(line));
        } finally {
            TestServer.stop(server, database);
        }
    }
}
