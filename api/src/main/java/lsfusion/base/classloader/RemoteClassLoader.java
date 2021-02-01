package lsfusion.base.classloader;

import lsfusion.base.ReflectionUtils;
import lsfusion.interop.logics.remote.RemoteLogicsInterface;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.rmi.server.RMIClassLoaderSpi;

import static lsfusion.base.ApiResourceBundle.getString;

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
            } catch (RemoteException remote) {
                throw new ClassNotFoundException(getString("errors.error.loading.class.on.the.client"), remote);
            }
        }

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
