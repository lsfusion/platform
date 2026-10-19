package lsfusion.gwt.server;

import lsfusion.base.file.RawFileData;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class JsxTransformerTest {

    // this tier only runs on the JVMs its JS engine supports (JsxTransformer.MIN_JAVA..MAX_JAVA); outside that
    // range every transform legitimately degrades to a console.error stub, so the suite is skipped rather than
    // reported as failure — a developer on a newer JDK than the platform's engine supports gets a skip with a
    // reason, not a wall of red
    @BeforeClass
    public static void engineSupportsThisJvm() {
        String spec = System.getProperty("java.specification.version", "");
        int major;
        try {
            major = Integer.parseInt(spec.startsWith("1.") ? spec.substring(2) : spec);
        } catch (NumberFormatException e) {
            return; // unrecognized scheme: run and let the assertions decide
        }
        assumeTrue("the lightweight .jsx tier's JS engine supports Java 11-23, this JVM is " + System.getProperty("java.version"),
                major >= 11 && major <= 23);
    }

    // the web server's temporary folder, where the transforms are kept between its starts
    @ClassRule
    public static final TemporaryFolder webServerTemp = new TemporaryFolder();

    @BeforeClass
    public static void webServerTempFolder() {
        FileUtils.APP_DOWNLOAD_FOLDER_PATH = webServerTemp.getRoot().getPath();
    }

    private static String transform(String name, String source) {
        return JsxTransformer.transform(name, new RawFileData(source, StandardCharsets.UTF_8)).getString(StandardCharsets.UTF_8);
    }

    // the memo alias and the compiler's cache helper are named per file (see memoAndCacheHelpersAreNamedPerFile), so a
    // test finds them by what they are bound to rather than by name
    private static String boundName(String code, String init) {
        Matcher matcher = Pattern.compile("(?m)^const (\\w+) = " + Pattern.quote(init) + ";").matcher(code);
        assertTrue("expected a top-level binding of " + init + ", got: " + code, matcher.find());
        return matcher.group(1);
    }

    @Test
    public void transformsJsxToClassicRuntime() {
        String code = transform("helloBoard.jsx",
                "function HelloBoard(props) {\n" +
                "    return <div className=\"hello-board\">{props.count} orders</div>;\n" +
                "}\n");
        assertTrue(code.contains("React.createElement"));
        assertFalse(code.contains("<div"));
        assertTrue(code.contains("function HelloBoard"));
    }

    @Test
    public void compilesAndAutoMemoizesCleanComponent() {
        String code = transform("card.jsx",
                "function Card(props) {\n" +
                "    const rows = props.items.map(i => <li key={i.id}>{i.name}</li>);\n" +
                "    return <ul className=\"card\">{rows}</ul>;\n" +
                "}\n");
        String cache = boundName(code, "window.lsfusion.rcRuntime.c"); // runtime import rewritten to the window shim
        assertTrue(code.contains(cache + "(")); // React Compiler memo cache in the component body
        assertTrue(code.contains("Card = " + boundName(code, "React.memo") + "(Card)")); // compiler-certified component auto-wrapped
        assertTrue(code.contains("React.createElement")); // JSX transformed in the same pass
        assertFalse(code.contains("import")); // classic script: no module syntax survives
    }

    @Test
    public void runtimeImportRewriteShapes() {
        // the shape the compiler emits — aliased named import — is rewritten, from BOTH runtime module names
        for (String module : new String[]{"react-compiler-runtime", "react/compiler-runtime"}) {
            String code = transform("runtime.jsx", "import { c as _c } from '" + module + "';\nwindow.x = _c;\n");
            // the binding is renamed per file, and every reference has to follow it
            assertTrue(code.contains("window.x = " + boundName(code, "window.lsfusion.rcRuntime.c") + ";"));
            assertFalse(code.contains("import"));
            assertFalse(code.startsWith("console.error("));
        }
        // degenerate shapes of those two module names can only be user code: the import must survive the
        // rewrite untouched and be rejected by the preflight with the tier message — never malformed output,
        // never a silent pass-through to the browser
        for (String degenerate : new String[]{
                "import rt from 'react-compiler-runtime';\nwindow.x = rt;\n", // default import
                "import * as rt from 'react/compiler-runtime';\nwindow.x = rt;\n", // namespace import
                "import 'react-compiler-runtime';\nwindow.x = 1;\n"}) { // side-effect-only import
            String code = transform("degenerate.jsx", degenerate);
            assertTrue(code.startsWith("console.error("));
            assertTrue(code.contains("src/main/web"));
        }
    }

    // #1805: every served .jsx is a classic script, and they all share one global lexical scope, so the memo alias and
    // the cache helper, declared under the same names in two files, made the second one throw and never run. a-b.jsx
    // and a_b.jsx read the same once '-' maps onto '_', so it is the digest of the exact name that has to tell them apart
    @Test
    public void memoAndCacheHelpersAreNamedPerFile() {
        String source = "function Label(props) { return <b>{props.text}</b>; }\n";
        String first = transform("a-b.jsx", source);
        String second = transform("a_b.jsx", source);
        for (String init : new String[]{"window.lsfusion.rcRuntime.c", "React.memo"})
            assertNotEquals(boundName(first, init), boundName(second, init));
    }

    @Test
    public void impureComponentBailsOutButStillTransforms() {
        String code = transform("impure.jsx",
                "function Impure(props) {\n" +
                "    window.renderCount = (window.renderCount || 0) + 1;\n" +
                "    return <div>{window.renderCount}</div>;\n" +
                "}\n");
        assertFalse(code.startsWith("console.error(")); // a bailout is not a failure: the script is served
        assertFalse(code.contains("rcRuntime")); // the compiler correctly skips the impure render: no memo cache
        assertFalse(code.contains("React.memo")); // auto-memo respects the bailout
        assertTrue(code.contains("React.createElement")); // JSX still transformed
    }

    @Test
    public void brokenJsxYieldsConsoleErrorStub() {
        String code = transform("broken.jsx", "function Broken() { return <div; }\n");
        assertTrue(code.startsWith("console.error("));
        assertTrue(code.contains("lsFusion .jsx broken.jsx failed to compile"));
        // babel's SyntaxError detail (line:column position) must survive the GraalJS host boundary — a bare
        // "SyntaxError" with no position would leave an application developer with no way to locate the defect
        assertTrue("expected a line:column position in the failure message, got: " + code, code.contains("(1:31)"));
    }

    @Test
    public void importIsRejectedWithTierMessage() {
        String code = transform("modular.jsx",
                "import React from 'react';\n" +
                "function Modular() { return <b>x</b>; }\n");
        assertTrue(code.startsWith("console.error("));
        assertTrue(code.contains("src/main/web"));
    }

    @Test
    public void cacheHitReturnsIdenticalResult() {
        String source = "function Cached() { return <span>cached</span>; }\n";
        RawFileData first = JsxTransformer.transform("cached.jsx", new RawFileData(source, StandardCharsets.UTF_8));
        RawFileData second = JsxTransformer.transform("cached.jsx", new RawFileData(source, StandardCharsets.UTF_8));
        assertSame(first, second);
    }

    @Test
    public void renames() {
        assertTrue(JsxTransformer.isJsx("web/init/helloBoard.jsx"));
        assertFalse(JsxTransformer.isJsx("web/init/helloBoard.js"));
        assertEquals("web/init/helloBoard.js", JsxTransformer.toJs("web/init/helloBoard.jsx"));
    }

    // #1907: the transforms were kept in memory only, so every start of the web server transformed again, and the first
    // page whose .jsx did not fit its wait - a large file takes minutes - got a stub. What an earlier start transformed
    // is served even to a page that cannot wait at all
    @Test
    public void aRestartServesWhatTheEarlierStartTransformed() throws Exception {
        String source = "function Restarted(props) { return <i>{props.text}</i>; }\n";
        String transformed = transform("restarted.jsx", source);
        assertFalse(transformed.startsWith("console.error("));

        Class<?> restarted = new Restart().loadClass(JsxTransformer.class.getName());
        restarted.getField("timeout").setInt(null, 0);
        restarted.getMethod("startPageBudget").invoke(null);
        try {
            RawFileData served = (RawFileData) restarted.getMethod("transform", String.class, RawFileData.class)
                    .invoke(null, "restarted.jsx", new RawFileData(source, StandardCharsets.UTF_8));
            assertEquals(transformed, served.getString(StandardCharsets.UTF_8));
        } finally {
            restarted.getMethod("endPageBudget").invoke(null);
        }
    }

    // how long a page waits is the web server's jsxTransformTimeout setting: a page it is too short for gets a stub that
    // names the setting, and the transform goes on for the pages after it
    @Test
    public void aPageWaitsAsLongAsTheTimeoutSetting() {
        String source = "function Impatient(props) { return <u>{props.text}</u>; }\n";
        int timeout = JsxTransformer.timeout;
        JsxTransformer.timeout = 0;
        String stub;
        try {
            JsxTransformer.startPageBudget();
            try {
                stub = transform("impatient.jsx", source);
            } finally {
                JsxTransformer.endPageBudget();
            }
        } finally {
            JsxTransformer.timeout = timeout;
        }
        assertTrue(stub.startsWith("console.error("));
        assertTrue(stub, stub.contains("still being compiled after a 0 s wait (the jsxTransformTimeout setting of the web server)"));
        assertFalse(transform("impatient.jsx", source).startsWith("console.error(")); // the stubbed page left the transform running
    }

    // the web server started again: a fresh copy of the transformer, so nothing it kept in memory, and everything else -
    // its temporary folder among it - the same
    private static class Restart extends ClassLoader {
        Restart() {
            super(JsxTransformerTest.class.getClassLoader());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            String transformer = JsxTransformer.class.getName();
            if (!name.equals(transformer) && !name.startsWith(transformer + "$"))
                return super.loadClass(name, resolve);
            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    try (InputStream code = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                        byte[] bytes = IOUtils.toByteArray(code);
                        loaded = defineClass(name, bytes, 0, bytes.length);
                    } catch (IOException e) {
                        throw new ClassNotFoundException(name, e);
                    }
                }
                return loaded;
            }
        }
    }
}
