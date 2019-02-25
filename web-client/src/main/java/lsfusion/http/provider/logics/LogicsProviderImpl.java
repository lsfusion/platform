package lsfusion.http.provider.logics;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
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
import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static lsfusion.base.BaseUtils.nvl;

// singleton, one for whole application
// needed for custom handler requests + RemoteAuthenticationManager (where gwt is not used)
public class LogicsProviderImpl implements InitializingBean, LogicsProvider {

    protected final static Logger logger = Logger.getLogger(LogicsProviderImpl.class);

    private static final String hostKey = "host";
    private static final String portKey = "port";
    private static final String exportNameKey = "exportName";

    private String host; // default host
    private int port; // default port
    private String exportName; // default export name

    public LogicsProviderImpl() {
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
        FileUtils.APP_IMAGES_FOLDER_URL = appPath + "/static/images/";
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

    private final Map<LogicsConnection, LogicsSessionObject> currentLogics = new ConcurrentHashMap<>();

    // not like in other providers, getter shouldn't be called directly to ensure invalidating reference if we get RemoteException
    private static LogicsSessionObject createLogicsSessionObject(LogicsConnection connection) throws AppServerNotAvailableException {
        LogicsSessionObject logicsSessionObject;
        RemoteLogicsInterface logics;
        try {
            RemoteLogicsLoaderInterface loader = RMIUtils.rmiLookup(connection.host, connection.port, connection.exportName, "RemoteLogicsLoader");
            logics = loader.getLogics();
        } catch (MalformedURLException e) {
            throw Throwables.propagate(e);
        } catch (NotBoundException | RemoteException e) {
            throw new AppServerNotAvailableException();
        }
        logicsSessionObject = new LogicsSessionObject(logics, connection);
        return logicsSessionObject;
    }

    private LogicsSessionObject createOrGetLogicsSessionObject(String host, Integer port, String exportName) throws AppServerNotAvailableException {
        LogicsConnection connection = new LogicsConnection(host != null ? host : this.host, port != null ? port : this.port, exportName != null ? exportName : this.exportName); 
        LogicsSessionObject logicsSessionObject = currentLogics.get(connection);
        if(logicsSessionObject == null) { // no sync, it's no big deal if we'll lost some cache
            logicsSessionObject = createLogicsSessionObject(connection);
            currentLogics.put(connection, logicsSessionObject);
        }
        return logicsSessionObject;
    }

    private void invalidateLogicsSessionObject(LogicsSessionObject sessionObject) { // should be called if logics remote method call fails with remoteException
        currentLogics.remove(sessionObject.connection);
    }
    
    public <R> R runRequest(String host, Integer port, String exportName, LogicsRunnable<R> runnable) throws AppServerNotAvailableException, RemoteException {
        LogicsSessionObject logicsSessionObject = createOrGetLogicsSessionObject(host, port, exportName);
        try {
            return runnable.run(logicsSessionObject);
        } catch (lsfusion.interop.exceptions.AuthenticationException e) {
            // if there is an AuthenticationException and server has anonymousUI, that means that the mode has changed, so we we'll drop serverSettings cache
            if(logicsSessionObject.serverSettings != null && logicsSessionObject.serverSettings.anonymousUI)
                logicsSessionObject.serverSettings = null;
            throw e;
        } catch (RemoteException e) { // it's important that this exception should not be suppressed (for example in ExternalRequestHandler)
            invalidateLogicsSessionObject(logicsSessionObject);
            throw e;
        }
    }

    @Override
    public ServerSettings getServerSettings(final HttpServletRequest request) {
        try {
            return runRequest(this, request, new LogicsRunnable<ServerSettings>() {
                public ServerSettings run(LogicsSessionObject sessionObject) throws RemoteException {
                    return sessionObject.getServerSettings(request);
                }
            });
        } catch (Exception e) {
            return null;
        }
    }

    public static <R> R runRequest(LogicsProvider logicsProvider, HttpServletRequest request, LogicsRunnable<R> runnable) throws AppServerNotAvailableException, RemoteException {
        return logicsProvider.runRequest(
                request != null ? request.getParameter("host") : null,
                request != null ? BaseUtils.parseInt(request.getParameter("port")) : null,
                request != null ? request.getParameter("exportName") : null,
                runnable);
    }
}
