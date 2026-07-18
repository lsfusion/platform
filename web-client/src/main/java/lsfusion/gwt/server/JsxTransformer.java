package lsfusion.gwt.server;

import lsfusion.base.file.RawFileData;
import org.apache.log4j.Logger;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

// Lightweight .jsx tier: a .jsx resource is transformed to plain js on the server when it is saved
// for serving (FileUtils.saveWebFile callers) and renamed to .js, so the browser receives an ordinary
// classic script — no client-side machinery. The transform is one Babel pass (a vendored
// babel-standalone + babel-plugin-react-compiler bundle, see babel-rc.PROVENANCE.md) run in an
// embedded GraalJS engine: JSX lowering (classic runtime, i.e. React.createElement against the
// platform window.React) plus React Compiler auto-memoization — the memo cache is read through the
// window.lsfusion.rcRuntime shim (a platform web resource loaded before any custom script), and
// compiler-certified components are additionally wrapped in React.memo. JSX here is only syntax
// sugar over the hand-written-global authoring model — no imports, no bundling; module graphs
// belong to the compiled src/main/web tier.
public class JsxTransformer {
    private static final Logger logger = Logger.getLogger(JsxTransformer.class);

    // RawFileData.getID() is content-addressed, so a changed file misses the cache; dynamic web
    // actions can feed unbounded unique content over the web client's lifetime, hence the crude
    // size cap (transforms are cheap and re-cached, an LRU is not worth the machinery here)
    private static final int CACHE_LIMIT = 1000;
    private static final ConcurrentHashMap<String, RawFileData> cache = new ConcurrentHashMap<>();

    // Babel's recursive-descent work needs a much bigger stack than the JVM default; instead of
    // relying on a global -Xss flag, all engine work runs on this dedicated 16MB-stack thread
    // (a single thread also satisfies the GraalJS single-threaded-context contract)
    private static final ExecutorService engineThread = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(null, runnable, "jsx-transform", 16L * 1024 * 1024);
        thread.setDaemon(true);
        return thread;
    });

    private static Value transformFunction; // lazy; access only on engineThread (transformOnEngineThread)

    public static boolean isJsx(String resourceName) {
        return resourceName.endsWith(".jsx");
    }

    public static String toJs(String resourceName) {
        assert isJsx(resourceName);
        return resourceName.substring(0, resourceName.length() - "x".length());
    }

    public static RawFileData transform(String resourceName, RawFileData source) {
        String id = source.getID();
        RawFileData cached = cache.get(id);
        if (cached != null)
            return cached;
        try {
            RawFileData result = new RawFileData(doTransform(source.getString(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
            if (cache.size() >= CACHE_LIMIT)
                cache.clear();
            cache.put(id, result);
            return result;
        } catch (Throwable t) { // a PolyglotException carries the Babel message and position
            String message = t.getMessage();
            if (message != null && MODULE_SYNTAX.matcher(message).find())
                message += "; imports are not supported in the lightweight .jsx tier; use src/main/web (compiled)";
            // a broken .jsx must not break the page render or the action queue: serve a stub that
            // reports to the browser console instead — the same contract as a failed classic script.
            // NOT cached: the stub embeds the resource name, so the same broken content under another
            // name must be re-reported with its own name
            return new RawFileData("console.error(" + JSONObject.quote("lsFusion .jsx transform failed for " + resourceName + ": " + message) + ");", StandardCharsets.UTF_8);
        }
    }

    private static final Pattern MODULE_SYNTAX = Pattern.compile("\\b(import|export|module)\\b", Pattern.CASE_INSENSITIVE);

    /** server JVMs this tier can run on: the Babel/GraalJS engine (org.graalvm.js 22.3.x) is compiled for Java 11,
     * so an older JVM cannot load it at all, and its Truffle calls sun.misc.Unsafe.ensureClassInitialized, which
     * Java 24 removed from sun.misc.Unsafe. Both ends lift together when the engine is upgraded (a newer GraalVM
     * line drops the Unsafe call but raises the classfile floor, so it needs the platform's own build floor to
     * rise first) — the same range applies to the compiled tier, see CompileWebMojo.RC_MIN_JAVA. */
    private static final int MIN_JAVA = 11;
    private static final int MAX_JAVA = 23;

    // Outside that range, touching a Graal class throws either an opaque UnsupportedClassVersionError (too old)
    // or a NoSuchMethodError from deep inside Truffle (too new). transform()'s catch already degrades a failed
    // .jsx to a console.error stub rather than breaking the page, so this only makes the stub's message
    // actionable instead of cryptic. Checked without loading any Graal class.
    private static void checkJavaVersion() {
        String spec = System.getProperty("java.specification.version", "");
        int major;
        try {
            major = Integer.parseInt(spec.startsWith("1.") ? spec.substring(2) : spec); // "1.8" -> 8, "11" -> 11
        } catch (NumberFormatException e) {
            return; // unrecognized scheme: don't block, let class loading decide
        }
        if (major < MIN_JAVA || major > MAX_JAVA)
            throw new RuntimeException("the lightweight .jsx tier requires the server to run on Java " + MIN_JAVA + "-" + MAX_JAVA
                    + " (its JS engine); current JVM is " + System.getProperty("java.version")
                    + " — run the server on Java " + MIN_JAVA + "-" + MAX_JAVA + ", or use a plain .js resource, or move the component"
                    + " to the compiled src/main/web tier (which has no server-side JVM requirement)");
    }

    // synchronized: serializing transforms is fine for this tier (rare, cached by content)
    private static synchronized String doTransform(String source) {
        checkJavaVersion(); // before any Graal class is touched, so an old JVM gets guidance, not UnsupportedClassVersionError
        try {
            return engineThread.submit(() -> transformOnEngineThread(source)).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) { // unwrap so transform() sees the raw PolyglotException message
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException)
                throw (RuntimeException) cause;
            if (cause instanceof Error)
                throw (Error) cause;
            throw new RuntimeException(cause);
        }
    }

    private static String transformOnEngineThread(String source) {
        if (transformFunction == null) {
            long started = System.currentTimeMillis();
            Context context = Context.newBuilder("js").option("engine.WarnInterpreterOnly", "false").build();
            // the bundle is a browser iife, and the compiler plugin also probes node globals
            context.eval("js", "var window = globalThis; var global = globalThis;" +
                    "var process = {env: {}, argv: [], platform: 'linux', version: 'v18.0.0', cwd: function() { return '/'; }," +
                    "               nextTick: function(f) { f(); }, stdout: {fd: 1, write: function() {}}, stderr: {fd: 2, write: function() {}}, stdin: {fd: 0}, browser: true};");
            context.eval("js", readBabelBundle());
            // the new Function preflight compiles (never calls) the result, rejecting what Babel
            // passes through untransformed — a user's top-level import/export (only the compiler's
            // own runtime imports are rewritten by the bundle) — and most syntax errors
            transformFunction = context.eval("js",
                    "(function(src) {" +
                    "    var code = rc.transform(src);" +
                    "    new Function(code);" +
                    "    return code;" +
                    "})");
            logger.info("lsFusion .jsx transformer initialized in " + (System.currentTimeMillis() - started) + " ms");
        }
        return transformFunction.execute(source).asString();
    }

    private static String readBabelBundle() {
        try (InputStream stream = JsxTransformer.class.getResourceAsStream("/lsfusion/jsx/babel-rc.min.js")) {
            return new RawFileData(stream).getString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("failed to read the Babel bundle", e);
        }
    }
}
