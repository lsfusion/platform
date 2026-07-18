package lsfusion.gwt.server;

import lsfusion.base.file.RawFileData;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

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

    private static String transform(String name, String source) {
        return JsxTransformer.transform(name, new RawFileData(source, StandardCharsets.UTF_8)).getString(StandardCharsets.UTF_8);
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
        assertTrue(code.contains("_c(")); // React Compiler memo cache in the component body
        assertTrue(code.contains("window.lsfusion.rcRuntime")); // runtime import rewritten to the window shim
        assertTrue(code.contains("React.memo")); // compiler-certified component auto-wrapped
        assertTrue(code.contains("_rcMemo(Card)"));
        assertTrue(code.contains("React.createElement")); // JSX transformed in the same pass
        assertFalse(code.contains("import")); // classic script: no module syntax survives
    }

    @Test
    public void runtimeImportRewriteShapes() {
        // the shape the compiler emits — aliased named import — is rewritten, from BOTH runtime module names
        for (String module : new String[]{"react-compiler-runtime", "react/compiler-runtime"}) {
            String code = transform("runtime.jsx", "import { c as _c } from '" + module + "';\nwindow.x = _c;\n");
            assertTrue(code.contains("const _c = window.lsfusion.rcRuntime.c;"));
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

    @Test
    public void impureComponentBailsOutButStillTransforms() {
        String code = transform("impure.jsx",
                "function Impure(props) {\n" +
                "    window.renderCount = (window.renderCount || 0) + 1;\n" +
                "    return <div>{window.renderCount}</div>;\n" +
                "}\n");
        assertFalse(code.startsWith("console.error(")); // a bailout is not a failure: the script is served
        assertFalse(code.contains("_c(")); // the compiler correctly skips the impure render
        assertFalse(code.contains("React.memo")); // auto-memo respects the bailout
        assertTrue(code.contains("React.createElement")); // JSX still transformed
    }

    @Test
    public void brokenJsxYieldsConsoleErrorStub() {
        String code = transform("broken.jsx", "function Broken() { return <div; }\n");
        assertTrue(code.startsWith("console.error("));
        assertTrue(code.contains("lsFusion .jsx transform failed for broken.jsx"));
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
}
