package platform.gwt.base.server.spring;

import org.apache.log4j.Logger;
import platform.interop.RemoteLogicsInterface;
import platform.interop.RemoteLogicsLoaderInterface;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SingleBusinessLogicsProvider<T extends RemoteLogicsInterface> implements BusinessLogicsProvider<T> {
    protected final static Logger logger = Logger.getLogger(SingleBusinessLogicsProvider.class);

    private final List<InvalidateListener> invlidateListeners = Collections.synchronizedList(new ArrayList<InvalidateListener>());

    private volatile T logics;
    private final Object logicsLock = new Object();

    private String serverHost;
    private int serverPort;

    public SingleBusinessLogicsProvider() {
    }

    public SingleBusinessLogicsProvider(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    public String getServerHost() {
        return serverHost;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public T getLogics() {
        //double-check locking
        if (logics == null) {
            synchronized (logicsLock) {
                if (logics == null) {
                    logics = (T) createRemoteLogics();
                }
            }
        }
        return logics;
    }

    private RemoteLogicsInterface createRemoteLogics() {
        try {
            Registry registry = LocateRegistry.getRegistry(serverHost, serverPort);
            //todo: setup dbName
            RemoteLogicsLoaderInterface loader = (RemoteLogicsLoaderInterface) registry.lookup("default/RemoteLogicsLoader");

            return loader.getLogics();
        } catch (Exception e) {
            logger.error("Ошибка при получении объекта логики: ", e);
            throw new RuntimeException("Произошла ошибка при подлючении к серверу приложения.", e);
        }
    }

    public void invalidate() {
        synchronized (logicsLock) {
            logics = null;

            for (InvalidateListener invalidateListener : invlidateListeners) {
                invalidateListener.onInvalidate();
            }
        }
    }

    @Override
    public void addInvlidateListener(InvalidateListener listener) {
        invlidateListeners.add(listener);
    }

    @Override
    public void removeInvlidateListener(InvalidateListener listener) {
        invlidateListeners.remove(listener);
    }
}
