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
import static lsfusion.tests.TestServer.read;
import static lsfusion.tests.TestServer.tail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Level 2, the compile corpus. The server runs with dryRun, which compiles the logic and exits before it opens
 *  a database connection. compile/ok has to compile; every compile/fail module has to fail with the lines its
 *  .expected lists. The ok modules go in one run, a failing one needs a run of its own - the log then holds the
 *  errors of that module and nothing else. */
@RunWith(Parameterized.class)
public class LsfCompileTest {

    private final String name;
    private final String includePaths;
    private final List<String> expected; // null for the ok corpus, which only has to compile

    public LsfCompileTest(String name, String includePaths, List<String> expected) {
        this.name = name;
        this.includePaths = includePaths;
        this.expected = expected;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> corpus() throws IOException {
        List<Object[]> cases = new ArrayList<>();
        cases.add(new Object[]{"ok", "/ok/*", null});

        Path fail = BASE.resolve("compile/fail");
        try (Stream<Path> modules = Files.walk(fail)) {
            for (Path module : (Iterable<Path>) modules.filter(path -> path.toString().endsWith(".lsf")).sorted()::iterator) {
                String name = fail.relativize(module).toString().replace('\\', '/');
                Path expected = module.resolveSibling(module.getFileName().toString().replace(".lsf", ".expected"));
                cases.add(new Object[]{"fail/" + name, "/fail/" + name, expectedLines(expected)});
            }
        }
        return cases;
    }

    private static List<String> expectedLines(Path expected) throws IOException {
        List<String> lines = new ArrayList<>();
        for (String line : Files.readAllLines(expected))
            if (!line.trim().isEmpty())
                lines.add(line.trim());
        return lines;
    }

    @Test
    public void compiles() throws Exception {
        Path log = BASE.resolve("target/compile-" + name.replace('/', '-') + ".log");
        int exitCode = TestServer.start(TestServer.classPath("compile"), log,
                "settings.dryRun=true", "logics.includePaths=" + includePaths).waitFor();
        String output = read(log);

        if (expected == null) {
            assertEquals(tail(output), 0, exitCode);
        } else {
            assertTrue("compiled, but was expected to fail\n" + tail(output), exitCode != 0);
            for (String line : expected)
                assertTrue("not among the errors reported: " + line + "\n" + tail(output), output.contains(line));
        }
    }
}
