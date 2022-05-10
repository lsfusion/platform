package lsfusion.base.classloader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

public class WriteUsedClassLoader extends ClassLoader {

    private final Map<String, byte[]> classes;

    public WriteUsedClassLoader(Map<String, byte[]> classes, ClassLoader originalClassloader) {
        super(originalClassloader);
        this.classes = classes;
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            return super.findClass(name);
        } catch (ClassNotFoundException cne) {
            byte[] buf = classes.get(name.replace(".", "/") + ".class");
            if (buf != null)
                return defineClass(name, buf, 0, buf.length);

            throw cne;
        }
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        // Load class from bytes. If class doesn't contain in the classes we assume it is in the client's classpath
        return classes.containsKey(name) ? new ByteArrayInputStream(classes.get(name)) : super.getResourceAsStream(name);
    }
}
