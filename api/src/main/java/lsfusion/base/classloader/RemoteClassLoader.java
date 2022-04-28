package lsfusion.base.classloader;

import com.google.common.base.Throwables;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.function.BiConsumer;

import static lsfusion.base.ApiResourceBundle.getString;

public class RemoteClassLoader extends ClassLoader {

    private Map<String, byte[]> classes;

    private BiConsumer<String, byte[]> classesMap;

    public RemoteClassLoader(BiConsumer<String, byte[]> classesMap, ClassLoader originalClassloader) {
        super(originalClassloader);
        this.classesMap = classesMap;
    }

    public RemoteClassLoader(Map<String, byte[]> classes, ClassLoader originalClassloader) {
        super(originalClassloader);
        this.classes = classes;
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            return super.findClass(name);
        } catch (ClassNotFoundException cne) {
            try {
                if (classes != null) {
                    byte[] buf = classes.get(name.replace(".", "/") + ".class");
                    return defineClass(name, buf, 0, buf.length);
                }
                return null;
            } catch (IllegalArgumentException remote) { // catch IllegalArgumentException to the JRClassloader work correctly. Because remoteLogics.findClass() throws IllegalArgumentException from getResource, but JRClassloader catches only ClassNotFoundException
                throw new ClassNotFoundException(getString("errors.error.loading.class.on.the.client"), remote);
            }
        }
    }

    // used in JasperCompileManager not in the desktop-client
    @Override
    public InputStream getResourceAsStream(String name) {
        InputStream resourceAsStream = super.getResourceAsStream(name);

        if (classes != null && classes.containsKey(name)) { // call from the client. Load class from bytes
            return new ByteArrayInputStream(classes.get(name));
        } else if (classesMap == null) // If class doesn't contain in the classes we assume it is in the client's classpath
            return resourceAsStream;
        else if (resourceAsStream != null) { //call from the server. Save classes that are used in .jrxml
            try {
                if (!name.startsWith("lsfusion/base") && !name.startsWith("java") && !name.startsWith("net/sf")) //save classes that will not be in the client's classpath
                    classesMap.accept(name, resourceAsStream.readAllBytes());
            } catch (IOException e) {
                Throwables.propagate(e);
            }
        }
        return super.getResourceAsStream(name);
    }

}
