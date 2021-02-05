package lsfusion.base.classloader;

import lsfusion.base.ReflectionUtils;
import lsfusion.interop.logics.remote.RemoteLogicsInterface;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.rmi.server.RMIClassLoaderSpi;

import static lsfusion.base.ApiResourceBundle.getString;

// The class is needed in order to pass its classes from the server not through codeBase, but directly through the RMI interface

// It turned out that the deserialization mechanism in RMI works very interestingly when paired with the standard serialization mechanism
// First, a chunk of the class descriptor (ObjectStreamClass) is loaded from the stream, containing the name, SUID, etc.
// Then the name goes resolveClass for the ObjectInputStream, which ends up in RMIClassLoaderSpi
// After that, from the resulting Class, a full descriptor of the localDesc class is compiled, which is compared with the serialized one in the stream
// At the same time, when the full class descriptor is loaded, the field classes are loaded without using the resolveClass of the ObjectInputStream
// Thus, NoClassDefFoundException immediately falls
// Accordingly, you need to either completely make your own ClassLoader or immediately load the implementation of all classes related by fields from the server

public class RemoteClassLoader extends RMIClassLoaderSpi {
    public static class ExternalClassLoader extends ClassLoader {

        private final RemoteLogicsInterface remoteLogics;

        public ExternalClassLoader(RemoteLogicsInterface remoteLogics) {
            super(Thread.currentThread().getContextClassLoader());
            this.remoteLogics = remoteLogics;
        }

        @Override
        public Class<?> findClass(String name) throws ClassNotFoundException {
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

        // used in JasperCompileManager not in the desktop-client
        @Override
        public InputStream getResourceAsStream(String name) {
            InputStream resourceAsStream = super.getResourceAsStream(name);
            if (resourceAsStream == null) {
                try {
                    resourceAsStream = new ByteArrayInputStream(remoteLogics.findClass(name.replace(".class", "")));
                } catch (IllegalArgumentException | RemoteException e) {
                    return null;
                }
            }
            return resourceAsStream;
        }
    }

    private final ExternalClassLoader remoteLoader;

    public RemoteClassLoader(RemoteLogicsInterface remoteLogicsInterface) {
        this.remoteLoader = new ExternalClassLoader(remoteLogicsInterface);
    }

    @Override
    public Class<?> loadClass(String codebase, String name, ClassLoader defaultLoader) throws MalformedURLException, ClassNotFoundException {
        try {
            Class loadHandlerClass = Class.forName("sun.rmi.server.LoaderHandler");
            return ReflectionUtils.getStaticMethodValue(loadHandlerClass, "loadClass",
                    new Class[] {String.class, String.class, ClassLoader.class}, new Object[]{codebase, name, defaultLoader});
        } catch (ClassNotFoundException ce) {
            if (remoteLoader.remoteLogics != null) {
                return remoteLoader.loadClass(name);
            } else
                throw ce;
        }
    }

    @Override
    public Class<?> loadProxyClass(String codebase, String[] interfaces, ClassLoader defaultLoader) throws MalformedURLException, ClassNotFoundException {
        try {
            Class loadHandlerClass = Class.forName("sun.rmi.server.LoaderHandler");
            return ReflectionUtils.getStaticMethodValue(loadHandlerClass, "loadProxyClass", new Class[] {String.class, String[].class, ClassLoader.class}, new Object[] {codebase, interfaces, defaultLoader});
        } catch (ClassNotFoundException ce) {
            if (remoteLoader.remoteLogics != null) {
                for (String iFace : interfaces) {
                    loadClass(codebase, iFace, defaultLoader);
                }
                Class loadHandlerClass = Class.forName("sun.rmi.server.LoaderHandler");
                return ReflectionUtils.getStaticMethodValue(loadHandlerClass, "loadProxyClass", new Class[] {String.class, String[].class, ClassLoader.class}, new Object[] {codebase, interfaces, remoteLoader});
            } else
                throw ce;
        }
    }

    @Override
    public ClassLoader getClassLoader(String codebase) throws MalformedURLException {
        Class loadHandlerClass = ReflectionUtils.classForName("sun.rmi.server.LoaderHandler");
        if(loadHandlerClass != null) {
            try {
                return ReflectionUtils.getStaticMethodValue(loadHandlerClass, "getClassLoader", new Class[]{String.class}, new Object[]{codebase});
            } catch (ClassNotFoundException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public String getClassAnnotation(Class<?> cl) {
        Class loadHandlerClass = ReflectionUtils.classForName("sun.rmi.server.LoaderHandler");
        if(loadHandlerClass != null) {
            try {
                return ReflectionUtils.getStaticMethodValue(loadHandlerClass, "getClassAnnotation", new Class[]{Class.class}, new Object[]{cl});
            } catch (ClassNotFoundException e) {
                return null;
            }
        } else {
            return null;
        }
    }
}
