package lsfusion.gwt.base.server.spring;

import com.google.gwt.core.client.GWT;
import lsfusion.base.Provider;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.RemoteLogicsLoaderInterface;
import lsfusion.interop.remote.RMIUtils;
import org.apache.log4j.Logger;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static lsfusion.base.BaseUtils.isRedundantString;

public class SingleBusinessLogicsProvider<T extends RemoteLogicsInterface> implements BusinessLogicsProvider<T> {
    protected final static Logger logger = Logger.getLogger(SingleBusinessLogicsProvider.class);

    private final List<InvalidateListener> invlidateListeners = Collections.synchronizedList(new ArrayList<InvalidateListener>());

    private final ReadWriteLock logicsLock = new ReentrantReadWriteLock();
    private final Lock readLogicsLock = logicsLock.readLock();
    private final Lock writeLogicsLock = logicsLock.writeLock();

    private volatile T logics;

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

    public boolean isSingleInstance() {
        try {
            return logics.isSingleInstance();
        } catch (RemoteException e) {
            return false;
        }
    }

    public T getLogics() {
        return synchronizedGet(new Provider<T>() {
            @Override
            public T get() {
                return logics;
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
            RemoteLogicsLoaderInterface loader = RMIUtils.rmiLookup(registryHost, registryPort, exportName, "RemoteLogicsLoader");

            logics = (T) loader.getLogics();
        } catch (Exception e) {
            logger.error("Ошибка при получении объекта логики: ", e);
            throw new RuntimeException("Произошла ошибка при подлючении к серверу приложения.", e);
        }
    }

    @Override
    public void invalidate() {
        try {
            GWT.log("Invalidating logics...", new Exception());
        } catch (NoClassDefFoundError ignored) {} // валится при попытке подключиться после перестарта сервера
        
        writeLogicsLock.lock();
        try {
            logics = null;

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
