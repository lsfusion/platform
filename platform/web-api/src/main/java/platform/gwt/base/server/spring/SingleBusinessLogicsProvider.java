package platform.gwt.base.server.spring;

import org.apache.log4j.Logger;
import platform.base.Provider;
import platform.interop.RemoteLogicsInterface;
import platform.interop.RemoteLogicsLoaderInterface;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static platform.base.BaseUtils.isRedundantString;

public class SingleBusinessLogicsProvider<T extends RemoteLogicsInterface> implements BusinessLogicsProvider<T> {
    protected final static Logger logger = Logger.getLogger(SingleBusinessLogicsProvider.class);

    private final List<InvalidateListener> invlidateListeners = Collections.synchronizedList(new ArrayList<InvalidateListener>());

    private final ReadWriteLock logicsLock = new ReentrantReadWriteLock();
    private final Lock readLogicsLock = logicsLock.readLock();
    private final Lock writeLogicsLock = logicsLock.writeLock();

    private volatile T logics;
    private volatile TimeZone timeZone;

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
        return synchronizedGet(new Provider<T>() {
            @Override
            public T get() {
                return logics;
            }
        });
    }

    @Override
    public TimeZone getTimeZone() {
        return synchronizedGet(new Provider<TimeZone>() {
            @Override
            public TimeZone get() {
                return timeZone;
            }
        });
    }

    private <T> T synchronizedGet(Provider<T> getter) {
        readLogicsLock.lock();

        //double-check locking
        if (logics == null) {
            readLogicsLock.unlock();

            writeLogicsLock.lock();
            try {
                if (logics == null) {
                    createRemoteLogics();
                }
                return getter.get();
            } finally {
                writeLogicsLock.unlock();
            }
        }
        try {
            return getter.get();
        } finally {
            readLogicsLock.unlock();
        }
    }

    private void createRemoteLogics() {
        try {
            Registry registry = LocateRegistry.getRegistry(registryHost, registryPort);
            RemoteLogicsLoaderInterface loader = (RemoteLogicsLoaderInterface) registry.lookup(exportName + "/RemoteLogicsLoader");

            logics = (T) loader.getLogics();
            timeZone = logics.getTimeZone();
        } catch (Exception e) {
            logger.error("Ошибка при получении объекта логики: ", e);
            throw new RuntimeException("Произошла ошибка при подлючении к серверу приложения.", e);
        }
    }

    public void invalidate() {
        writeLogicsLock.lock();
        try {
            logics = null;
            timeZone = null;

            for (InvalidateListener invalidateListener : invlidateListeners) {
                invalidateListener.onInvalidate();
            }
        } finally {
            writeLogicsLock.unlock();
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
