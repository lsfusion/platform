package lsfusion.tests;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

// starts the platform's bootstrap in a jvm of its own, with the command line production uses and the jvm options a
// runner adds
class TestServer {
    static final Path BASE = Paths.get("").toAbsolutePath();

    private static final String DB_SERVER = System.getProperty("db.server", "localhost");
    private static final String DB_USER = System.getProperty("db.user", "postgres");
    private static final String DB_PASSWORD = System.getProperty("db.password", "");

    // what maven resolved for this module (written by dependency:build-classpath), plus the roots holding the
    // .lsf under test - which of those the server sees is exactly what the two runners differ in
    static String classPath(String... roots) throws IOException {
        StringBuilder classPath = new StringBuilder(read(BASE.resolve("target/server-classpath.txt")));
        for (String root : roots)
            classPath.append(File.pathSeparatorChar).append(BASE.resolve(root));
        return classPath.toString();
    }

    // the test logics on a database of its own and on the given ports - http, rmi, websocket, debugger - all of them
    // free ones, as whoever runs this usually has a server of their own up
    static Process start(String database, int[] ports, Path log, String... settings) throws IOException {
        List<String> properties = new ArrayList<>(Arrays.asList(
                "db.name=" + database, "db.server=" + DB_SERVER, "db.user=" + DB_USER, "db.password=" + DB_PASSWORD,
                "http.port=" + ports[0], "rmi.port=" + ports[1], "webSocket.port=" + ports[2], "debugger.port=" + ports[3]));
        properties.addAll(Arrays.asList(settings));
        return start(classPath("target/classes"), log, Collections.emptyList(), properties.toArray(new String[0]));
    }

    interface Check {
        boolean done() throws Exception;
    }

    static void waitFor(Check check, Process process, Path log, String failure) throws Exception {
        long deadline = System.currentTimeMillis() + Duration.ofMinutes(10).toMillis();
        while (!check.done()) {
            if (!process.isAlive() || System.currentTimeMillis() > deadline)
                throw new IllegalStateException(failure + "\n" + excerpt(read(log)));
            Thread.sleep(250);
        }
    }

    static void waitStarted(Process server, Path log) throws Exception {
        waitFor(() -> read(log).contains("Server has successfully started"), server, log, "the test server did not start");
    }

    static void stop(Process server, String database) throws Exception {
        server.destroyForcibly(); // not destroy() : a server a test left deadlocked would never finish an orderly shutdown
        server.waitFor();

        if (Boolean.getBoolean("lsf.keepDb")) {
            System.out.println("kept the test database " + database);
            return;
        }
        try (Connection connection = DriverManager.getConnection("jdbc:postgresql://" + DB_SERVER + "/postgres", DB_USER, DB_PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS " + database + " WITH (FORCE)");
        }
    }

    // all open at once : taken one by one, each closed before the next is opened, two of them could get the same port
    static int[] freePorts(int count) throws IOException {
        List<ServerSocket> sockets = new ArrayList<>();
        try {
            for (int i = 0; i < count; i++)
                sockets.add(new ServerSocket(0));
            return sockets.stream().mapToInt(ServerSocket::getLocalPort).toArray();
        } finally {
            for (ServerSocket socket : sockets)
                socket.close();
        }
    }

    // the tests a failure of which is reported as skipped, see quarantine.txt
    static Set<String> quarantine() throws IOException {
        Set<String> quarantined = new HashSet<>();
        for (String line : Files.readAllLines(BASE.resolve("quarantine.txt")))
            if (!line.trim().isEmpty() && !line.trim().startsWith("#"))
                quarantined.add(line.trim());
        return quarantined;
    }

    static Process start(String classPath, Path log, List<String> jvmOptions, String... properties) throws IOException {
        List<String> command = new ArrayList<>(Arrays.asList(
                Paths.get(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", classPath,
                "--add-opens=java.base/java.util=ALL-UNNAMED",
                "--add-opens=java.base/java.lang=ALL-UNNAMED"));
        command.addAll(jvmOptions);
        for (String property : properties)
            command.add("-D" + property);
        command.add("lsfusion.server.logics.BusinessLogicsBootstrap");

        Files.createDirectories(log.getParent());
        return new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
    }

    static String read(Path file) throws IOException {
        return new String(Files.readAllBytes(file), Charset.defaultCharset());
    }

    // the two ways the server reports it could not start: LogicsInstance.start logs so every exception it stops on,
    // BusinessLogicsBootstrap.start a spring context that could not be built
    private static final Pattern START_FAILURE = Pattern.compile("Exception while starting logics instance|Error creating logics instance");

    // what a failure quotes of the server's log: its last 50 lines, and above them, when it lies further up, the first
    // report of why the server could not start - under a long stack trace the end holds nothing of it but frames. Only
    // that report is looked for, not any error: the server logs errors it goes on after too (the tess4j one of every full
    // start, an ambiguous implementation), and not every failure gets one (a jvm that did not start, a hang). Lines are
    // cut at 500 chars, as the log opens with the whole classpath on one line
    static String excerpt(String output) {
        List<String> lines = new ArrayList<>();
        for (String line : output.split("\n"))
            lines.add(line.length() > 500 ? line.substring(0, 500) + " ..." : line);
        int tail = Math.max(0, lines.size() - 50);
        String end = String.join("\n", lines.subList(tail, lines.size()));

        int failure = 0;
        while (failure < tail && !START_FAILURE.matcher(lines.get(failure)).find())
            failure++;
        if (failure == tail) // no report, or it is in the end anyway
            return end;
        int failureEnd = Math.min(failure + 20, tail);
        return String.join("\n", lines.subList(failure, failureEnd)) + "\n" + (failureEnd < tail ? "... " + (tail - failureEnd) + " lines ...\n" : "") + end;
    }
}
