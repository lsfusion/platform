package lsfusion.server.physics.dev.integration.internal.to;

import org.junit.Test;

import java.util.concurrent.Callable;

import static org.junit.Assert.*;

public class InternalCompilerTest {

    @Test
    public void compilesPackagedClass() throws Exception {
        Class<?> cls = InternalCompiler.compile("test.pkg.Simple",
                "package test.pkg;\n" +
                "public class Simple {\n" +
                "    public String hello() { return \"hello\"; }\n" +
                "}");
        assertEquals("test.pkg.Simple", cls.getName());
        Object instance = cls.getConstructor().newInstance();
        assertEquals("hello", cls.getMethod("hello").invoke(instance));
    }

    @Test
    public void compilesLambdaAndGenerics() throws Exception { // Janino, replaced by ECJ, could not compile this
        Class<?> cls = InternalCompiler.compile("LambdaUser",
                "import java.util.concurrent.Callable;\n" +
                "public class LambdaUser implements Callable<String> {\n" +
                "    public String call() throws Exception {\n" +
                "        java.util.function.Supplier<String> s = () -> \"lambda\";\n" +
                "        return s.get();\n" +
                "    }\n" +
                "}");
        assertEquals("lambda", ((Callable<?>) cls.getConstructor().newInstance()).call());
    }

    @Test
    public void referencesClasspathClass() throws Exception {
        Class<?> cls = InternalCompiler.compile("UsesServerClass",
                "public class UsesServerClass {\n" +
                "    public String path() { return lsfusion.server.base.ResourceUtils.getClassPath() == null ? \"null\" : \"ok\"; }\n" +
                "}");
        assertEquals("ok", cls.getMethod("path").invoke(cls.getConstructor().newInstance()));
    }

    @Test
    public void reportsCompileErrorWithLine() {
        try {
            InternalCompiler.compile("Broken", "public class Broken {\n    int x = ;\n}");
            fail("expected compilation failure");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("line 2"));
        }
    }

    @Test
    public void reportsPackageMismatch() {
        try {
            InternalCompiler.compile("expected.pkg.Name", "package actual.pkg;\npublic class Name {}");
            fail("expected package mismatch failure");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("expected.pkg.Name"));
        }
    }

    @Test
    public void cachedCompileSharesClassIdentity() {
        String source = "public class CachedOnce {}";
        assertSame(InternalCompiler.compileCached("CachedOnce", source), InternalCompiler.compileCached("CachedOnce", source));
    }
}
