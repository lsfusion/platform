package lsfusion.tests;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

// starts the platform's bootstrap in a jvm of its own, with the command line production uses
class TestServer {
    static final Path BASE = Paths.get("").toAbsolutePath();

    // what maven resolved for this module (written by dependency:build-classpath), plus the roots holding the
    // .lsf under test - which of those the server sees is exactly what the two runners differ in
    static String classPath(String... roots) throws IOException {
        StringBuilder classPath = new StringBuilder(read(BASE.resolve("target/server-classpath.txt")));
        for (String root : roots)
            classPath.append(File.pathSeparatorChar).append(BASE.resolve(root));
        return classPath.toString();
    }

    static Process start(String classPath, Path log, String... properties) throws IOException {
        List<String> command = new ArrayList<>(Arrays.asList(
                Paths.get(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", classPath,
                "--add-opens=java.base/java.util=ALL-UNNAMED",
                "--add-opens=java.base/java.lang=ALL-UNNAMED"));
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
