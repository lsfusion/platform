package platform.client;

import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.rmi.server.RMIClassLoaderSpi;

// Класс нужен для того, чтобы передавать с сервера его классы не через codeBase, а через непосредственно RMI интерфейс

// Выяснилось, что механизм десериализации в RMI работает очень интересно в паре со стандартным механизмом сериализации
// Сначала из потока загружается кусок дескриптора класса (ObjectStreamClass), содержащий имя, SUID и т.д.
// Затем по имени идет resolveClass для ObjectInputStream, который в конечном итоге приходит в RMIClassLoaderSpi
// После этого из полученного Class составляется полный дескриптор класса localDesc, который сравнивается с сериализованным в потоке
// При этом при загрузке полного дескриптора класса классы полей загружаются уже без использования resolveClass у ObjectInputStream
// Таким образом тут же падает NoClassDefFoundException

// Сейчас проблема решена таким образом, что всем загруженным классам сервера выставляется в качестве ClassLoader - ClientRMIClassLoaderSpi.RemoteClassLoader
// Таким образом все классы полей загруженных с сервера проходят через него и спрашивают свою реализацию у сервера
public class ClientRMIClassLoaderSpi extends RMIClassLoaderSpi {
    private static class RemoteClassLoader extends ClassLoader {
        public RemoteClassLoader(ClassLoader original) {
            super(original);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            try {
                byte[] classBytes = Main.remoteLoader.findClass(name);
                return defineClass(name, classBytes, 0, classBytes.length);
            } catch (RemoteException remote) {
                throw new ClassNotFoundException(ClientResourceBundle.getString("errors.error.loading.class.on.the.client"), remote);
            }
        }
    }
    
    private static ClassLoader remoteLoader = new RemoteClassLoader(ClientRMIClassLoaderSpi.class.getClassLoader());

    @Override
    public Class<?> loadClass(String codebase, String name, ClassLoader defaultLoader) throws MalformedURLException, ClassNotFoundException {
        try {
            return sun.rmi.server.LoaderHandler.loadClass(codebase, name, defaultLoader);
        } catch (ClassNotFoundException ce) {
            if (Main.remoteLoader != null) {
                return remoteLoader.loadClass(name);
            } else
                throw ce;
        }
    }

    @Override
    public Class<?> loadProxyClass(String codebase, String[] interfaces, ClassLoader defaultLoader) throws MalformedURLException, ClassNotFoundException {
        try {
            return sun.rmi.server.LoaderHandler.loadProxyClass(codebase, interfaces, defaultLoader);
        } catch (ClassNotFoundException ce) {
            if (Main.remoteLoader != null) {
                for (String iFace : interfaces) {
                    loadClass(codebase, iFace, defaultLoader);
                }
                return sun.rmi.server.LoaderHandler.loadProxyClass(codebase, interfaces, remoteLoader);
            } else
                throw ce;
        }
    }

    @Override
    public ClassLoader getClassLoader(String codebase) throws MalformedURLException {
        return sun.rmi.server.LoaderHandler.getClassLoader(codebase);
    }

    @Override
    public String getClassAnnotation(Class<?> cl) {
        return sun.rmi.server.LoaderHandler.getClassAnnotation(cl);
    }
}
