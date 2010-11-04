package platform.client;

import java.lang.reflect.Method;
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
// Соответственно нужно или полностью делать свой ClassLoader либо с сервера сразу же загружать реализацию всех связанных по полям классов
public class ClientRMIClassLoaderSpi extends RMIClassLoaderSpi {

    private static final Class[] parameters = new Class[]{String.class, byte[].class, int.class, int.class};

    @Override
    public Class<?> loadClass(String codebase, String name, ClassLoader defaultLoader) throws MalformedURLException, ClassNotFoundException {
        try {
            return sun.rmi.server.LoaderHandler.loadClass(codebase, name, defaultLoader);
        } catch (ClassNotFoundException ce) {
            if (Main.remoteLogics != null) {
                try {
                    // Необходимо, чтобы класс загружался именно с ClassLoader'ом JavaWS, иначе не будет находить классов в основном jar-файле
                    byte[] classDef = Main.remoteLogics.findClass(name);
                    ClassLoader loader = ClientRMIClassLoaderSpi.class.getClassLoader();
                    try {
                        Method defineClass = ClassLoader.class.getDeclaredMethod("defineClass", parameters);
                        defineClass.setAccessible(true);
                        return (Class<?>)defineClass.invoke(loader, name, classDef, 0, classDef.length);
                    } catch (Exception se) {
                        throw new ClassNotFoundException("Ошибка при загрузке класса на клиенте", se); 
                    }
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
