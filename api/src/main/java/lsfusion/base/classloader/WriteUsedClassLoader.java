package lsfusion.base.classloader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

import static lsfusion.base.ApiResourceBundle.getString;

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
            try {
                byte[] buf = classes.get(name.replace(".", "/") + ".class");
                return defineClass(name, buf, 0, buf.length);
            } catch (IllegalArgumentException remote) { // catch IllegalArgumentException to the JRClassloader work correctly. Because remoteLogics.findClass() throws IllegalArgumentException from getResource, but JRClassloader catches only ClassNotFoundException
                throw new ClassNotFoundException(getString("errors.error.loading.class.on.the.client"), remote);
            }
        }
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        // Load class from bytes. If class doesn't contain in the classes we assume it is in the client's classpath
        return classes.containsKey(name) ? new ByteArrayInputStream(classes.get(name)) : super.getResourceAsStream(name);
    }
}
