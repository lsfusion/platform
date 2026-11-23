package lsfusion.server.base;

import lsfusion.server.base.caches.*;
import lsfusion.server.base.controller.stack.StackMessage;
import lsfusion.server.base.controller.stack.StackProgress;
import lsfusion.server.base.controller.thread.AssertSynchronized;
import lsfusion.server.base.version.NFLazy;
import lsfusion.server.data.pack.PackComplex;
import org.springframework.asm.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// The server's caches, stacks and locks are aspects, woven into its classes by the AspectJ compiler as Maven builds
// them. A class compiled by a plain javac - an IDE building the server's module itself, into the directory Maven builds
// it into - runs without them: an @IdentityStrongLazy method makes a new element on every call, and the start fails
// on the names they share, far from the cause. So a server run from a directory of classes checks first that they are
// woven, and refuses to start naming the classes that are not. A jar is built by Maven, and is not checked
public class AspectWeaving {

    // the annotations an aspect weaves into every method they mark that has a body and a this: the pointcuts are
    // execution(@A * *.*(..)) && target(Object). Those of ContextTwin and Pack match only Query's methods, so a method
    // of another class they mark is never woven, and are left out
    private static final Set<String> WOVEN = descriptors(IdentityLazy.class, IdentityStartLazy.class, IdentityInstanceLazy.class,
            IdentityStrongLazy.class, IdentityStrongNotNullLazy.class, IdentityQuickLazy.class, ParamLazy.class,
            ParamInstanceLazy.class, ParamTwinLazy.class, SynchronizedLazy.class, TwinLazy.class, AssertSynchronized.class,
            NFLazy.class);
    // and those whose pointcuts are execution(@A * *.*(..)) alone, which weave into a static method too
    private static final Set<String> WOVEN_STATIC = descriptors(StackMessage.class, StackProgress.class, PackComplex.class);

    private static Set<String> descriptors(Class<?>... annotations) {
        return Stream.of(annotations).map(Type::getDescriptor).collect(Collectors.toSet());
    }

    // the attribute the AspectJ weaver gives a class it has woven an aspect into
    private static final String WEAVER_STATE = "org.aspectj.weaver.WeaverState";

    public static void check() throws IOException, URISyntaxException {
        URL location = AspectWeaving.class.getProtectionDomain().getCodeSource().getLocation();
        if (!location.getProtocol().equals("file")) // a jar nested in another one, as an application server may unpack it
            return;
        Path classes = Paths.get(location.toURI());
        if (!Files.isDirectory(classes))
            return;

        List<String> unwoven = findUnwoven(classes);
        if (!unwoven.isEmpty())
            throw new IllegalStateException("The server's classes in " + classes + " are compiled without the AspectJ aspects woven into them, "
                    + "so their caches, stacks and locks do not work: " + String.join(", ", unwoven.subList(0, Math.min(10, unwoven.size())))
                    + (unwoven.size() > 10 ? " and " + (unwoven.size() - 10) + " more" : "")
                    + ". It happens when an IDE compiles them with javac: compile them with Maven (mvn clean compile on the server's module), "
                    + "or with the AspectJ compiler in the IDE (in IntelliJ IDEA, Ajc as the Java compiler, or the build delegated to Maven)");
    }

    // the classes under the directory with a method an aspect weaves into, but not woven
    private static List<String> findUnwoven(Path classes) throws IOException {
        List<Path> files;
        try (Stream<Path> walk = Files.walk(classes)) {
            files = walk.filter(file -> file.toString().endsWith(".class")).collect(Collectors.toList());
        }
        List<String> unwoven = new ArrayList<>();
        for (Path file : files) {
            ClassReader reader;
            try (InputStream input = Files.newInputStream(file)) {
                reader = new ClassReader(input);
            } catch (IllegalArgumentException e) {
                // a class file of a Java newer than this ASM reads: Maven builds the server for Java 8, so the javac of
                // a newer JDK compiled it, unwoven
                String name = classes.relativize(file).toString();
                unwoven.add(name.substring(0, name.length() - ".class".length()).replace(file.getFileSystem().getSeparator(), "."));
                continue;
            }
            Checker checker = new Checker();
            reader.accept(checker, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            if (checker.marked && !checker.woven)
                unwoven.add(reader.getClassName().replace('/', '.'));
        }
        Collections.sort(unwoven);
        return unwoven;
    }

    private static class Checker extends ClassVisitor {
        boolean marked; // a method has a body and an annotation of WOVEN_STATIC, or of WOVEN and a this
        boolean woven;

        Checker() {
            super(SpringAsmInfo.ASM_VERSION);
        }

        @Override
        public void visitAttribute(Attribute attribute) {
            if (attribute.type.equals(WEAVER_STATE))
                woven = true;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            if ((access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0)
                return null;
            boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;
            return new MethodVisitor(SpringAsmInfo.ASM_VERSION) {
                @Override
                public AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
                    if (WOVEN_STATIC.contains(annotation) || (!isStatic && WOVEN.contains(annotation)))
                        marked = true;
                    return null;
                }
            };
        }
    }
}
