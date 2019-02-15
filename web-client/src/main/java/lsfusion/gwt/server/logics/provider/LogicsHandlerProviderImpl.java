package lsfusion.gwt.server.logics.provider;

import com.google.common.base.Throwables;
import com.google.gwt.core.client.GWT;
import lsfusion.gwt.server.FileUtils;
import lsfusion.gwt.server.logics.LogicsConnection;
import lsfusion.gwt.shared.exceptions.AppServerNotAvailableException;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.RemoteLogicsLoaderInterface;
import lsfusion.interop.remote.RMIUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletContext;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static lsfusion.base.BaseUtils.nvl;

// singleton, one for whole application
// needed for custom handler requests + RemoteAuthenticationManager (where gwt is not used)
public class LogicsHandlerProviderImpl<T extends RemoteLogicsInterface> implements InitializingBean, LogicsHandlerProvider {

    protected final static Logger logger = Logger.getLogger(LogicsHandlerProviderImpl.class);

    private static final String hostKey = "host";
    private static final String portKey = "port";
    private static final String exportNameKey = "exportName";

    private String host;
    private int port;
    private String exportName;

    public LogicsHandlerProviderImpl() {
    }

    @Autowired
    private ServletContext servletContext;

    @Override
    public void afterPropertiesSet() throws Exception {
        String host = servletContext.getInitParameter(hostKey);
        String port = servletContext.getInitParameter(portKey);
        String exportName = nvl(servletContext.getInitParameter(exportNameKey), "default");
        if (host == null || port == null) {
            throw new IllegalStateException(hostKey + " or " + portKey + " parameters aren't set in web.xml");
        }

        String appPath = servletContext.getRealPath("");
        FileUtils.APP_IMAGES_FOLDER_URL = appPath + "/images/";
        FileUtils.APP_TEMP_FOLDER_URL = appPath + "/WEB-INF/temp";

        RMIUtils.initRMI();
        RMIUtils.overrideRMIHostName(host);
        
        setHost(host);
        setPort(Integer.parseInt(port));
        setExportName(exportName);
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getExportName() {
        return exportName;
    }

    public void setExportName(String exportName) {
        this.exportName = exportName;
    }

    @Override
    public LogicsConnection getLogicsConnection(String host, Integer port, String exportName) {
        return new LogicsConnection(host != null ? host : this.host, port != null ? port : this.port, exportName != null ? exportName : this.exportName);
    }

    private static class LogicsCache<T extends RemoteLogicsInterface> {
        private final ReadWriteLock logicsLock = new ReentrantReadWriteLock();
        private final Lock readLogicsLock = logicsLock.readLock();
        private final Lock writeLogicsLock = logicsLock.writeLock();
        private volatile T logics;

        public T getLogics(LogicsConnection connection) throws AppServerNotAvailableException {
            readLogicsLock.lock();

            //double-check locking
            if (logics == null) {
                readLogicsLock.unlock();

                writeLogicsLock.lock();
                try {
                    if (logics == null) {
                        try {
                            RemoteLogicsLoaderInterface loader = RMIUtils.rmiLookup(connection.host, connection.port, connection.exportName, "RemoteLogicsLoader");
                            this.logics = (T) loader.getLogics();
                        } catch (MalformedURLException e) {
                            throw Throwables.propagate(e);
                        } catch (NotBoundException | RemoteException e) {
                            throw new AppServerNotAvailableException();
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

        public void invalidate() {
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
    }

    private final Map<LogicsConnection, LogicsCache> logicsCaches = new ConcurrentHashMap<>();

    @Override
    public RemoteLogicsInterface getLogics(LogicsConnection connection) throws AppServerNotAvailableException {
        LogicsCache logicsCache = logicsCaches.get(connection);
        if(logicsCache == null) { // it's no big deal if we'll lost some cache
            logicsCache = new LogicsCache();
            logicsCaches.put(connection, logicsCache);
        }
        return logicsCache.getLogics(connection);
    }

    @Override
    public void invalidate(LogicsConnection connection) { // should be called if logics remote method call fails with remoteException
        try {
            GWT.log("Invalidating logics...", new Exception());
        } catch (Throwable ignored) {} // валится при попытке подключиться после перестарта сервера

        logicsCaches.get(connection).invalidate();
    }
}
