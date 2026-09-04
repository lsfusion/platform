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

    // the log opens with the whole classpath on one line, so a failure has to be reported from its end
    static String tail(String output) {
        List<String> lines = Arrays.asList(output.split("\n"));
        return String.join("\n", lines.subList(Math.max(0, lines.size() - 50), lines.size()));
    }
}
