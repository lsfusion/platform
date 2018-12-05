package lsfusion.gwt.form.server.logics.spring;

import com.google.gwt.core.client.GWT;
import lsfusion.gwt.base.server.spring.InvalidateListener;
import lsfusion.interop.RemoteLogicsLoaderInterface;
import lsfusion.interop.remote.RMIUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import lsfusion.interop.RemoteLogicsInterface;

import javax.servlet.ServletContext;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static lsfusion.base.BaseUtils.nvl;

// singleton, one for whole application
// needed for custom handler requests + RemoteAuthentificationManager (where gwt is not used)
public class LogicsHandlerProviderImpl<T extends RemoteLogicsInterface> implements InitializingBean, LogicsHandlerProvider<T> {

    protected final static Logger logger = Logger.getLogger(LogicsHandlerProviderImpl.class);

    private String registryHostKey = "registryHost";
    private String registryPortKey = "registryPort";
    private String exportNameKey = "exportName";

    private String registryHost;
    private int registryPort;
    private String exportName;

    public LogicsHandlerProviderImpl() {
    }

    @Autowired
    private ServletContext servletContext;

    @Override
    public void afterPropertiesSet() throws Exception {
        String registryHost = servletContext.getInitParameter(registryHostKey);
        String registryPort = servletContext.getInitParameter(registryPortKey);
        String exportName = nvl(servletContext.getInitParameter(exportNameKey), "default");
        if (registryHost == null || registryPort == null) {
            throw new IllegalStateException(registryHostKey + " or " + registryPortKey + " parameters aren't set in web.xml");
        }

        setRegistryHost(registryHost);
        setRegistryPort(Integer.parseInt(registryPort));
        setExportName(exportName);
    }

    public void setRegistryHostKey(String registryHostKey) {
        this.registryHostKey = registryHostKey;
    }

    public void setRegistryPortKey(String registryPortKey) {
        this.registryPortKey = registryPortKey;
    }

    public void setExportNameKey(String exportNameKey) {
        this.exportNameKey = exportNameKey;
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

    private final ReadWriteLock logicsLock = new ReentrantReadWriteLock();
    private final Lock readLogicsLock = logicsLock.readLock();
    private final Lock writeLogicsLock = logicsLock.writeLock();
    private volatile T logics;
    public T getLogics() throws RemoteException {
        readLogicsLock.lock();

        //double-check locking
        if (logics == null) {
            readLogicsLock.unlock();

            writeLogicsLock.lock();
            try {
                if (logics == null) {
                    try {
                        this.logics = lookupLogics(null, null, null);
                    } catch (NotBoundException | MalformedURLException e) {
                        logger.error("Ошибка при получении объекта логики: ", e);
                        throw new RuntimeException("Произошла ошибка при подлючении к серверу приложения.", e);
                    }
                }
                return logics;
            } finally {
                writeLogicsLock.unlock();
            }
        }
        try {
            return logics;
        } finally {
            readLogicsLock.unlock();
        }
    }

    @Override
    public void invalidate() { // should be called if logics remote method call fails with remoteException
        try {
            GWT.log("Invalidating logics...", new Exception());
        } catch (Throwable ignored) {} // валится при попытке подключиться после перестарта сервера

        writeLogicsLock.lock();
        try {
            logics = null;
        } finally {
            writeLogicsLock.unlock();
        }
    }

    // thread-safe, needed for default params
    private T lookupLogics(String host, Integer port, String exportName) throws RemoteException, NotBoundException, MalformedURLException {
        RemoteLogicsLoaderInterface loader = RMIUtils.rmiLookup(host != null ? host : registryHost, port != null ? port : registryPort, exportName != null ? exportName : this.exportName, "RemoteLogicsLoader");
        return (T) loader.getLogics();
    }
}
