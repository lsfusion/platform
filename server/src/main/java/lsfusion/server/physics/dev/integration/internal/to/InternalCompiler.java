package lsfusion.server.physics.dev.integration.internal.to;

import org.apache.commons.io.IOUtils;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.batch.CompilationUnit;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// compiles INTERNAL action sources (inline <{ }> code and .java resources) fully in memory;
// ECJ is used instead of the JDK compiler so the server keeps running without a JDK, same
// reasoning as the explicit ecj dependency shipped for JasperReports (api/pom.xml), and through
// its internal API - the same way JasperReports and Tomcat's JSP compiler use it - because ECJ's
// javax.tools facade resolves sources as physical files; dependencies are resolved through the
// platform classloader, the same one that parents the loader of the compiled classes.
// This shares the exact ECJ contract (Compiler/INameEnvironment/ICompilerRequestor) with Jasper's
// JRJdtCompiler, so the pinned ecj version is bumped together with the reports compiler path.
public class InternalCompiler {

    private static final ConcurrentHashMap<String, Class<?>> cachedClasses = new ConcurrentHashMap<>();

    // class-name form: several INTERNAL declarations of the same class have to share one Class identity
    public static Class<?> compileCached(String className, String source) {
        return cachedClasses.computeIfAbsent(className, name -> compile(name, source));
    }

    public static Class<?> compile(String className, String source) {
        Map<String, byte[]> classBytes = compileToBytes(className, source);

        if (!classBytes.containsKey(className))
            throw new RuntimeException("compiled source declares " + classBytes.keySet() + " instead of " + className + " (the package in the source file has to match the class name in the declaration)");

        ClassLoader classLoader = new ClassLoader(InternalCompiler.class.getClassLoader()) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                byte[] bytes = classBytes.get(name);
                if (bytes == null)
                    throw new ClassNotFoundException(name);
                return defineClass(name, bytes, 0, bytes.length);
            }
        };
        try {
            return classLoader.loadClass(className);
        } catch (ClassNotFoundException e) { // the bytes are there, checked above
            throw new RuntimeException(e);
        }
    }

    private static Map<String, byte[]> compileToBytes(String className, String source) {
        ICompilationUnit unit = new CompilationUnit(source.toCharArray(), className.replace('.', '/') + ".java", "UTF-8");

        Map<String, String> settings = new HashMap<>();
        settings.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_8);
        settings.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_8);
        settings.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_8);

        Map<String, byte[]> classBytes = new HashMap<>();
        StringBuilder problems = new StringBuilder();
        ICompilerRequestor requestor = result -> {
            if (result.hasErrors()) {
                for (CategorizedProblem problem : result.getErrors())
                    problems.append("\nline ").append(problem.getSourceLineNumber()).append(": ").append(problem.getMessage());
            } else {
                for (ClassFile classFile : result.getClassFiles())
                    classBytes.put(join(classFile.getCompoundName()), classFile.getBytes());
            }
        };

        new Compiler(new ClassLoaderEnvironment(className, unit), DefaultErrorHandlingPolicies.proceedWithAllProblems(),
                new CompilerOptions(settings), requestor, new DefaultProblemFactory(Locale.ENGLISH))
                .compile(new ICompilationUnit[]{unit});

        if (problems.length() > 0)
            throw new RuntimeException("compilation of " + className + " failed:" + problems);
        return classBytes;
    }

    private static String join(char[][] parts) {
        if (parts == null)
            return "";
        StringBuilder name = new StringBuilder();
        for (char[] part : parts) {
            if (name.length() > 0)
                name.append('.');
            name.append(part);
        }
        return name.toString();
    }

    private static class ClassLoaderEnvironment implements INameEnvironment {
        private final String mainClassName;
        private final ICompilationUnit unit;

        ClassLoaderEnvironment(String mainClassName, ICompilationUnit unit) {
            this.mainClassName = mainClassName;
            this.unit = unit;
        }

        @Override
        public NameEnvironmentAnswer findType(char[][] compoundTypeName) {
            return findType(join(compoundTypeName));
        }

        @Override
        public NameEnvironmentAnswer findType(char[] typeName, char[][] packageName) {
            String packagePrefix = join(packageName);
            return findType(packagePrefix.isEmpty() ? new String(typeName) : packagePrefix + "." + new String(typeName));
        }

        private NameEnvironmentAnswer findType(String name) {
            if (name.equals(mainClassName))
                return new NameEnvironmentAnswer(unit, null);
            try (InputStream classStream = InternalCompiler.class.getClassLoader().getResourceAsStream(name.replace('.', '/') + ".class")) {
                if (classStream == null)
                    return null;
                return new NameEnvironmentAnswer(new ClassFileReader(IOUtils.toByteArray(classStream), name.toCharArray(), true), null);
            } catch (IOException | ClassFormatException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean isPackage(char[][] parentPackageName, char[] packageName) {
            String parentPrefix = join(parentPackageName);
            String name = parentPrefix.isEmpty() ? new String(packageName) : parentPrefix + "." + new String(packageName);
            if (name.equals(mainClassName))
                return false;
            return InternalCompiler.class.getClassLoader().getResource(name.replace('.', '/') + ".class") == null;
        }

        @Override
        public void cleanup() {
        }
    }
}
