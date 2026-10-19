package lsfusion.gwt.server;

import lsfusion.base.SystemUtils;
import lsfusion.base.file.RawFileData;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.CompletableFuture;
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

    // ONE transform in flight per content key. The cache used to be consulted once BEFORE a class-wide lock and never
    // again after it, so every user whose first page followed a restart missed the cache, queued on that lock, and
    // then ran a FULL transform of the SAME content in turn - N users, N transforms, one after another. Now they all
    // wait on the first one's result. Work still happens on the single engine thread; nothing is parallelised
    private static final ConcurrentHashMap<String, CompletableFuture<RawFileData>> flights = new ConcurrentHashMap<>();

    // seconds a page waits for the transforms of its resources: the web server's jsxTransformTimeout setting. A page build
    // transforms every init resource in a row, so a per-resource wait would still let /main wait for their SUM. The
    // budget is for the whole build, and what is not ready inside it is served as a stub while the transform carries on
    // - a page that says so beats a request thread parked for minutes
    public static int timeout = 30;
    // nanoTime, not the wall clock: a clock stepped backwards between opening the budget and reading what is left of
    // it would turn 30 seconds into minutes, which is the wait this exists to bound
    private static final ThreadLocal<Long> pageDeadline = new ThreadLocal<>();
    public static void startPageBudget() {
        pageDeadline.set(System.nanoTime() + TimeUnit.SECONDS.toNanos(timeout));
    }
    public static void endPageBudget() {
        pageDeadline.remove();
    }
    private static long remainingWait() {
        Long until = pageDeadline.get();
        return until == null ? TimeUnit.SECONDS.toNanos(timeout) : Math.max(0, until - System.nanoTime());
    }

    // a large file takes minutes to transform, and the memory is empty after every start of the web server, so the
    // results are also kept on disk: in the web server's temporary folder, which Tomcat keeps across restarts and clears
    // when the application is redeployed. They lie under the fingerprint of what produced them - the Babel bundle and
    // this class - so a platform update never serves what an earlier one produced, and drops it
    private static class Store {
        static final File folder = new File(FileUtils.APP_DOWNLOAD_FOLDER_PATH, "jsx/" + fingerprint());

        static {
            File[] fingerprints = folder.getParentFile().listFiles();
            if (fingerprints != null)
                for (File stale : fingerprints)
                    if (!stale.equals(folder))
                        org.apache.commons.io.FileUtils.deleteQuietly(stale);
            File[] files = folder.listFiles(); // what a write that never finished left behind
            if (files != null)
                for (File file : files)
                    if (file.getName().endsWith(".tmp"))
                        org.apache.commons.io.FileUtils.deleteQuietly(file);
            folder.mkdirs();
        }

        private static String fingerprint() {
            try (InputStream bundle = JsxTransformer.class.getResourceAsStream(BUNDLE);
                 InputStream code = JsxTransformer.class.getResourceAsStream(JsxTransformer.class.getSimpleName() + ".class")) {
                ByteArrayOutputStream both = new ByteArrayOutputStream();
                IOUtils.copy(bundle, both);
                IOUtils.copy(code, both);
                return SystemUtils.generateID(both.toByteArray());
            } catch (IOException e) {
                throw new RuntimeException("failed to read the Babel bundle", e);
            }
        }

        static File file(String id) {
            return new File(folder, SystemUtils.generateID(id.getBytes(StandardCharsets.UTF_8)) + ".js");
        }
    }

    // the disk is only a cache: a result it cannot give back is transformed again
    private static RawFileData readStored(String id) {
        File file = Store.file(id);
        if (!file.exists())
            return null;
        try {
            return new RawFileData(file);
        } catch (IOException e) {
            logger.warn("lsFusion .jsx: failed to read the stored transform " + file, e);
            return null;
        }
    }

    // written to a file of its own and renamed, so a reader never gets half of it - a crash in the middle leaves only
    // the temporary file, which the next start deletes
    private static void store(String id, RawFileData result) {
        File file = Store.file(id);
        File written = null;
        try {
            written = File.createTempFile(file.getName(), ".tmp", Store.folder);
            result.write(written);
            Files.move(written.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            logger.warn("lsFusion .jsx: failed to store the transform " + file + "; it is kept in memory only", e);
            if (written != null)
                written.delete();
        }
    }

    private static void remember(String id, RawFileData result) {
        if (cache.size() >= CACHE_LIMIT)
            cache.clear();
        cache.put(id, result);
    }

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

    // every served .jsx is a plain classic script, and they all share ONE global lexical scope - so the helpers the
    // transform generates (the memo alias and the compiler's cache helper) must be named per FILE, or the second file
    // re-declares them, throws, and never runs. Derived from the resource NAME rather than its content: two files with
    // the same content are still two scripts. The readable part is only a hint - it maps many characters onto '_', so
    // the digest of the exact name is what actually keeps two names apart (a-b.jsx and a_b.jsx read the same otherwise)
    private static String token(String resourceName) {
        StringBuilder readable = new StringBuilder();
        for (int i = 0; i < resourceName.length(); i++) {
            char c = resourceName.charAt(i);
            readable.append(Character.isLetterOrDigit(c) || c == '_' ? c : '_');
        }
        return readable + "_" + digest(resourceName);
    }

    // LETTERS only, no digits: babel's generateUid strips trailing digits from the name it is given, so a hex digest
    // would be silently truncated - and two names whose digests differed only in those digits would collide again
    private static String digest(String resourceName) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(resourceName.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                result.append((char) ('a' + ((hash[i] >> 4) & 0xF)));
                result.append((char) ('a' + (hash[i] & 0xF)));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // SHA-256 is required of every JRE
        }
    }

    public static RawFileData transform(String resourceName, RawFileData source) {
        // the name is part of the key because it is part of the OUTPUT (see token) - the same content under two names
        // must not be served the other one's helper names
        String id = resourceName + "|" + source.getID();
        RawFileData cached = cache.get(id);
        if (cached != null)
            return cached;

        // read here rather than on the engine thread, which may be busy with another file for minutes
        RawFileData stored = readStored(id);
        if (stored != null) {
            remember(id, stored);
            return stored;
        }

        try {
            return flight(id, resourceName, source).get(remainingWait(), TimeUnit.NANOSECONDS);
        } catch (TimeoutException e) {
            // NOT a failure: the transform is still running and will publish its result, this page just cannot wait
            // for it any longer. The flight is deliberately left alone, so the next request joins it instead of
            // starting a second one, and the load after that is served from the cache
            return stub(resourceName, "is still being compiled after a " + timeout + " s wait (the jsxTransformTimeout setting of the web server); reload the page in a moment");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return stub(resourceName, "was interrupted while compiling");
        } catch (ExecutionException e) { // a PolyglotException carries the Babel message and position
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            String message = cause.getMessage();
            if (message != null && MODULE_SYNTAX.matcher(message).find())
                message += "; imports are not supported in the lightweight .jsx tier; use src/main/web (compiled)";
            return stub(resourceName, "failed to compile: " + message);
        }
    }

    // a broken (or not yet ready) .jsx must not break the page render or the action queue: serve a script that
    // reports to the browser console instead - the same contract as a failed classic script. NEVER cached: the
    // reason is about this attempt, and the resource must be able to come back on the next one
    private static RawFileData stub(String resourceName, String what) {
        return new RawFileData("console.error(" + JSONObject.quote("lsFusion .jsx " + resourceName + " " + what) + ");", StandardCharsets.UTF_8);
    }

    private static CompletableFuture<RawFileData> flight(String id, String resourceName, RawFileData source) {
        CompletableFuture<RawFileData> running = flights.get(id);
        if (running != null)
            return running;

        CompletableFuture<RawFileData> created = new CompletableFuture<>();
        running = flights.putIfAbsent(id, created);
        if (running != null) // someone else registered first; join them rather than starting a second transform
            return running;

        // between the cache lookup above and this registration another flight may have published AND retired
        RawFileData published = cache.get(id);
        if (published != null) {
            created.complete(published);
            flights.remove(id, created);
            return created;
        }

        try {
            engineThread.execute(() -> runFlight(id, resourceName, source, created));
        } catch (RuntimeException e) { // the executor refused it - nobody may be left waiting on a task that will never run
            created.completeExceptionally(e);
            flights.remove(id, created);
        }
        return created;
    }

    private static void runFlight(String id, String resourceName, RawFileData source, CompletableFuture<RawFileData> flight) {
        try {
            checkJavaVersion(); // before any Graal class is touched, so an old JVM gets guidance, not UnsupportedClassVersionError
            long started = System.nanoTime();
            RawFileData result = new RawFileData(PREAMBLE + transformOnEngineThread(source.getString(StandardCharsets.UTF_8), token(resourceName)), StandardCharsets.UTF_8);
            long took = System.nanoTime() - started;
            String compiled = "lsFusion .jsx " + resourceName + " compiled in " + TimeUnit.NANOSECONDS.toMillis(took) + " ms";
            if (took > TimeUnit.SECONDS.toNanos(timeout)) // what it takes to choose the setting, so it is seen at the default log level
                logger.error(compiled + ", longer than a page waits for it: " + timeout + " s, the jsxTransformTimeout setting of the web server");
            else
                logger.info(compiled);
            store(id, result); // before anyone gets it, so what a page got a restart finds too
            remember(id, result); // published BEFORE the flight retires, so a late joiner finds the result either way
            flight.complete(result);
        } catch (Throwable t) {
            flight.completeExceptionally(t); // every waiter gets the same reason and reports it for itself
        } finally {
            flights.remove(id, flight); // conditional: only ours, and a failed one must be retryable next request
        }
    }

    // install the hooks (window.lsfusion.List / useData / Image / ...) BEFORE this file's body runs, so that a
    // module-top `const List = window.lsfusion.List` alias resolves - they are defined by lsfusion-custom-registry.js
    // (loaded earlier) but installed lazily, at the first React mount, which is long after this file executes. The
    // compiled tier gets the same preamble from its esbuild banner, see CompileWebMojo; without it here the two tiers
    // would disagree about whether that alias works. Installed against the now-final window.React: any app override of
    // it runs at a less-negative order than this file's 100.
    private static final String PREAMBLE = "if(window.lsfusion&&window.lsfusion.__installReactHooks)window.lsfusion.__installReactHooks();\n";

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
            throw new RuntimeException("the lightweight .jsx tier requires the web server to run on Java " + MIN_JAVA + "-" + MAX_JAVA
                    + " (its JS engine); current JVM is " + System.getProperty("java.version")
                    + " — run the web server on Java " + MIN_JAVA + "-" + MAX_JAVA + ", or use a plain .js resource, or move the component"
                    + " to the compiled src/main/web tier (which has no server-side JVM requirement)");
    }


    private static String transformOnEngineThread(String source, String token) {
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
                    "(function(src, token) {" +
                    "    var code = rc.transform(src, token);" +
                    "    new Function(code);" +
                    "    return code;" +
                    "})");
            logger.info("lsFusion .jsx transformer initialized in " + (System.currentTimeMillis() - started) + " ms");
        }
        return transformFunction.execute(source, token).asString();
    }

    private static final String BUNDLE = "/lsfusion/jsx/babel-rc.min.js";

    private static String readBabelBundle() {
        try (InputStream stream = JsxTransformer.class.getResourceAsStream(BUNDLE)) {
            return new RawFileData(stream).getString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("failed to read the Babel bundle", e);
        }
    }
}
