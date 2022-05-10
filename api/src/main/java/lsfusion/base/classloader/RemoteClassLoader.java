package lsfusion.base.classloader;

import lsfusion.interop.logics.remote.RemoteLogicsInterface;
import java.rmi.RemoteException;

import static lsfusion.base.ApiResourceBundle.getString;

public class RemoteClassLoader extends ClassLoader {

    private RemoteLogicsInterface remoteLogics;
    public void setRemoteLogics(RemoteLogicsInterface logics) {
        remoteLogics = logics;
    }

    public RemoteClassLoader(ClassLoader cl) {
        super(cl);
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            return super.findClass(name);
        } catch (ClassNotFoundException cne) {
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
}
