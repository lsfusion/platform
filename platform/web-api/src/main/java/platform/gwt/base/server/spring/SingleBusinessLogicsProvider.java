package platform.gwt.base.server.spring;

import org.apache.log4j.Logger;
import platform.interop.RemoteLogicsInterface;
import platform.interop.RemoteLogicsLoaderInterface;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static platform.base.BaseUtils.isRedundantString;

public class SingleBusinessLogicsProvider<T extends RemoteLogicsInterface> implements BusinessLogicsProvider<T> {
    protected final static Logger logger = Logger.getLogger(SingleBusinessLogicsProvider.class);

    private final List<InvalidateListener> invlidateListeners = Collections.synchronizedList(new ArrayList<InvalidateListener>());

    private volatile T logics;
    private final Object logicsLock = new Object();

    private String registryHost;
    private int registryPort;
    private String exportName;

    public SingleBusinessLogicsProvider() {
    }

    public SingleBusinessLogicsProvider(String registryHost, int registryPort, String exportName) {
        this.registryHost = registryHost;
        this.registryPort = registryPort;
        this.exportName = exportName;
        if (isRedundantString(exportName)) {
            throw new IllegalArgumentException("exportName must not be null");
        }
    }

    public String getRegistryHost() {
        return registryHost;
    }

    public void setRegistryHost(String registryHost) {
        this.registryHost = registryHost;
    }

    public int getRegistryPort() {
        return registryPort;
    }

    public void setRegistryPort(int registryPort) {
        this.registryPort = registryPort;
    }

    public String getExportName() {
        return exportName;
    }

    public void setExportName(String exportName) {
        this.exportName = exportName;
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
            Registry registry = LocateRegistry.getRegistry(registryHost, registryPort);
            RemoteLogicsLoaderInterface loader = (RemoteLogicsLoaderInterface) registry.lookup(exportName + "/RemoteLogicsLoader");

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
    public void addInvalidateListener(InvalidateListener listener) {
        invlidateListeners.add(listener);
    }

    @Override
    public void removeInvalidateListener(InvalidateListener listener) {
        invlidateListeners.remove(listener);
    }
}
