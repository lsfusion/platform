package lsfusion.build.web;

import org.apache.maven.plugin.MojoExecutionException;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * React Compiler in-process on GraalJS. One JVM-wide dedicated thread owns a single polyglot Context
 * that evaluates the vendored bundle once (rc/rc-graal.cjs exposing rcTransform(source, filename, target) —
 * rebuilt only by bin/build-rc-graal.mjs, pinned by rc/PROVENANCE) and then serves every transform: the
 * ~3s bundle evaluation is paid once per JVM, not per module of a multi-module build. Funneling ALL access
 * through that one thread satisfies GraalJS's single-threaded context rule, and the thread is created with
 * an explicit 16MB stack because Babel's recursive traversal overflows the JVM default — this must not
 * depend on -Xss flags of the surrounding build JVM. The lock serializes concurrent modules (mvn -T).
 *
 * Lifecycle is a deliberate warm-cache trade-off: the executor/context stay alive for the JVM so a
 * long-lived build JVM (mvnd, IDE-embedded Maven) keeps the engine warm across builds instead of re-paying
 * the bundle evaluation; the retained memory is bounded (one engine + the evaluated 5.3MB bundle). The
 * shutdown hook releases both on JVM exit so nothing outlives the process by design, not by leak.
 *
 * This class is the ONLY place Graal types appear: CompileWebMojo's own declared-member surface stays free of
 * Java-17-only classes, so on an old JVM nothing (including reflective member enumeration) can trip over
 * them before CompileWebMojo.checkRcJavaVersion has run — this class is loaded only when a transform executes.
 */
class RcEngine {

    private static final Object lock = new Object();
    private static ExecutorService executor;
    private static Context context;
    private static Value transformFn;

    static String transform(final String source, final String fileName, final String target, String displayName) throws MojoExecutionException, InterruptedException {
        synchronized (lock) {
            if (executor == null) {
                executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(null, r, "web-compile-rc", 16L * 1024 * 1024); // big stack: babel recursion
                        t.setDaemon(true); // engine stays warm for the JVM's lifetime without blocking its exit
                        return t;
                    }
                });
                Runtime.getRuntime().addShutdownHook(new Thread("web-compile-rc-shutdown") {
                    public void run() { // explicit release of the warm engine on JVM exit (see the lifecycle note above)
                        synchronized (lock) {
                            executor.shutdown();
                            if (context != null)
                                try {
                                    context.close(true); // cancel: nothing may still run at shutdown, but never block exit
                                } catch (Throwable ignored) { // best-effort cleanup must not disturb JVM shutdown
                                }
                        }
                    }
                });
            }
            try {
                return executor.submit(new Callable<String>() {
                    public String call() throws Exception {
                        if (transformFn == null) {
                            Context ctx = Context.newBuilder("js")
                                    .option("engine.WarnInterpreterOnly", "false").build(); // interpreted Truffle is expected on a stock JDK
                            // browser-ish host preamble the bundle expects (babel-standalone probes window/process)
                            ctx.eval("js", "var window = globalThis; var global = globalThis;"
                                    + " var process = {env: {}, argv: [], platform: 'linux', version: 'v18.0.0', cwd: function() { return '/'; },"
                                    + " nextTick: function(f) { f(); }, stdout: {fd: 1, write: function() {}}, stderr: {fd: 2, write: function() {}}, stdin: {fd: 0}, browser: true};");
                            try (InputStream is = RcEngine.class.getClassLoader().getResourceAsStream("rc/rc-graal.cjs")) {
                                if (is == null)
                                    throw new MojoExecutionException("bundled resource not found: rc/rc-graal.cjs");
                                ctx.eval(Source.newBuilder("js",
                                        new String(CompileWebMojo.readAll(is), StandardCharsets.UTF_8), "rc-graal.cjs").build());
                            }
                            transformFn = ctx.getBindings("js").getMember("rcTransform");
                            context = ctx; // published under the outer lock; the shutdown hook closes it
                        }
                        return transformFn.execute(source, fileName, target).asString();
                    }
                }).get();
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                throw new MojoExecutionException("react-compiler failed on " + displayName + ": " + cause.getMessage(), cause);
            }
        }
    }
}
