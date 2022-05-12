package lsfusion.base.classloader;

import lsfusion.interop.logics.remote.RemoteLogicsInterface;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.Map;

import static lsfusion.base.ApiResourceBundle.getString;

public class WriteUsedClassLoader extends ClassLoader {

    private final Map<String, byte[]> classes;
    private final RemoteLogicsInterface remoteLogics;

    public WriteUsedClassLoader(Map<String, byte[]> classes, ClassLoader originalClassloader, RemoteLogicsInterface remoteLogics) {
        super(originalClassloader);
        this.classes = classes;
        this.remoteLogics = remoteLogics;
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            return super.findClass(name);
        } catch (ClassNotFoundException cne) {
            byte[] buf = classes.get(name.replace(".", "/") + ".class");
            if (buf != null)
                return defineClass(name, buf, 0, buf.length);

            try {
                Class<?> aClass = findLoadedClass(name);
                byte[] classBytes;
                if (aClass == null) {
                    classBytes = remoteLogics.findClass(name);
                    aClass = defineClass(name, classBytes, 0, classBytes.length);
                }
                return aClass;
            } catch (RemoteException | IllegalArgumentException remote) { // catch IllegalArgumentException to the JRClassloader work correctly. Because remoteLogics.findClass() throws IllegalArgumentException from getResource, but JRClassloader catches only ClassNotFoundException
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
