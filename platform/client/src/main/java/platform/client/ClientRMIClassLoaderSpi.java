package platform.client;

import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.rmi.server.RMIClassLoaderSpi;

// Класс нужен для того, чтобы передавать с сервера его классы не через codeBase, а через непосредственно RMI интерфейс
public class ClientRMIClassLoaderSpi extends RMIClassLoaderSpi {

    @Override
    public Class<?> loadClass(String codebase, String name, ClassLoader defaultLoader) throws MalformedURLException, ClassNotFoundException {
        try {
            return sun.rmi.server.LoaderHandler.loadClass(codebase, name, defaultLoader);
        } catch (ClassNotFoundException ce) {
            if (Main.remoteLogics != null) {
                try {
                    return new ClassLoader() {
                        public Class<?> defineClass(byte[] b) {
                            return defineClass(b, 0, b.length);
                        }
                    }.defineClass(Main.remoteLogics.findClass(name));
                } catch (RemoteException re) {
                    throw new ClassNotFoundException("Ошибка при получении класса с сервера", re);
                }
            } else
                throw ce;
        }
    }

    @Override
    public Class<?> loadProxyClass(String codebase, String[] interfaces, ClassLoader defaultLoader) throws MalformedURLException, ClassNotFoundException {
        return sun.rmi.server.LoaderHandler.loadProxyClass(codebase, interfaces, defaultLoader);
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
