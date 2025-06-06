package lsfusion.http.provider.logics;

import lsfusion.base.BaseUtils;
import lsfusion.client.controller.remote.proxy.RemoteLogicsLoaderProxy;
import lsfusion.gwt.client.base.exception.AppServerNotAvailableDispatchException;
import lsfusion.gwt.server.FileUtils;
import lsfusion.gwt.server.convert.ClientFormChangesToGwtConverter;
import lsfusion.http.provider.navigator.NavigatorProviderImpl;
import lsfusion.interop.base.exception.AppServerNotAvailableException;
import lsfusion.interop.logics.*;
import lsfusion.interop.logics.remote.RemoteClientInterface;
import lsfusion.interop.logics.remote.RemoteLogicsLoaderInterface;
import lsfusion.interop.session.SessionInfo;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

// singleton, one for whole application
// needed for custom handler requests + RemoteAuthenticationManager (where gwt is not used)
public class LogicsProviderImpl extends AbstractLogicsProviderImpl implements InitializingBean, LogicsProvider {

    protected final static Logger logger = Logger.getLogger(LogicsProviderImpl.class);

    private static final String hostKey = "host";
    private static final String portKey = "port";
    private static final String exportNameKey = "exportName";

    private String host; // default host
    private int port; // default port
    private String exportName; // default export name

    public LogicsProviderImpl(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    private final ServletContext servletContext;

    private String getSystemProperty(String propertyName) {
        String property = System.getProperty(propertyName);
        if (property == null)
            property = System.getenv(propertyName.toUpperCase().replaceAll("\\.", "_"));
        return property;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        String host = servletContext.getInitParameter(hostKey);
        String hostProperty = getSystemProperty("app.server");
        if (hostProperty != null)
            host = hostProperty;

        String port = servletContext.getInitParameter(portKey);
        String portProperty = getSystemProperty("app.port");
        if (portProperty != null)
            port = portProperty;

        String exportName = servletContext.getInitParameter(exportNameKey);
        String exportNameProperty = getSystemProperty("app.export.name");
        if (exportNameProperty != null)
            exportName = exportNameProperty;

        if (host == null || port == null || exportName == null)
            throw new IllegalStateException(hostKey + " or " + portKey + " or " + exportNameKey + " parameters aren't set in web.xml");

        FileUtils.APP_CONTEXT_FOLDER_PATH = servletContext.getRealPath("");
        String tempDir = ((File) servletContext.getAttribute(ServletContext.TEMPDIR)).getPath(); // appPath + "/WEB-INF/temp";
        FileUtils.APP_DOWNLOAD_FOLDER_PATH = tempDir;
        FileUtils.APP_UPLOAD_FOLDER_PATH = tempDir;

        setHost(host);
        setPort(Integer.parseInt(port));
        setExportName(exportName);
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setExportName(String exportName) {
        this.exportName = exportName;
    }

    // needed to recode exception
    private <R> R runRequestDispatch(LogicsConnection logicsConnection, LogicsRunnable<R> runnable) throws RemoteException, AppServerNotAvailableDispatchException {
        try {
            return runRequest(logicsConnection, runnable);
        } catch (AppServerNotAvailableException e) {
            throw new AppServerNotAvailableDispatchException(e.getMessage());
        }
    }

    private LogicsConnection getLogicsConnection(String host, Integer port, String exportName) {
        return new LogicsConnection(host != null ? host : this.host, port != null ? port : this.port, exportName != null ? exportName : this.exportName);
    }

    private LogicsConnection getLogicsConnection(HttpServletRequest request) {
        return getLogicsConnection(request != null ? request.getParameter("host") : null,
                                   request != null ? BaseUtils.parseInt(request.getParameter("port")) : null,
                                   request != null ? request.getParameter("exportName") : null);
    }

    public static ServerSettings getServerSettings(HttpServletRequest request, boolean noCache, LogicsSessionObject sessionObject) throws RemoteException {
        return getServerSettings(sessionObject, NavigatorProviderImpl.getSessionInfo(request), request.getContextPath(), request.getServletContext(), noCache);
    }

    public static ServerSettings getServerSettings(LogicsSessionObject sessionObject, SessionInfo sessionInfo, String contextPath, ServletContext servletContext, boolean noCache) throws RemoteException {
        return sessionObject.getServerSettings(sessionInfo, contextPath, noCache, ClientFormChangesToGwtConverter.getConvertFileValue(servletContext, null)); // we need to use null because of the infinite recursion
    }

    public ServerSettings getServerSettings(final HttpServletRequest request, boolean noCache) {
        try {
            return runRequest(getLogicsConnection(request), (sessionObject, retry) -> getServerSettings(request, noCache, sessionObject));
        } catch (Throwable t) {
            return null;
        }
    }

    public <R> R runRequest(String host, Integer port, String exportName, LogicsRunnable<R> runnable) throws RemoteException, AppServerNotAvailableDispatchException {
        return runRequestDispatch(getLogicsConnection(host, port, exportName), runnable);
    }

    public <R> R runRequest(HttpServletRequest request, LogicsRunnable<R> runnable) throws RemoteException, AppServerNotAvailableDispatchException {
        return runRequestDispatch(getLogicsConnection(request), runnable);
    }

    @Override
    protected LogicsSessionObject createLogicsSessionObject(LogicsConnection connection) throws AppServerNotAvailableException, RemoteException {
        LogicsSessionObject result = super.createLogicsSessionObject(connection);

        result.remoteLogics.registerClient(new RemoteClient(result));

        return result;
    }

    public class RemoteClient extends UnicastRemoteObject implements RemoteClientInterface {

        public final LogicsSessionObject sessionObject;

        public RemoteClient(LogicsSessionObject sessionObject) throws RemoteException {
            super(0);
            this.sessionObject = sessionObject;
        }

        public String[] convertFileValue(SessionInfo sessionInfo, Serializable[] files) throws RemoteException {
            return ClientFormChangesToGwtConverter.convertFileValue(files, servletContext, LogicsProviderImpl.getServerSettings(sessionObject, sessionInfo, null, servletContext, false));
        }
    }

    @Override
    protected RemoteLogicsLoaderInterface lookupLoader(LogicsConnection connection) throws RemoteException, NotBoundException, MalformedURLException {
        return new RemoteLogicsLoaderProxy(super.lookupLoader(connection), connection.host);
    }

    @Override
    public void resetServerSettingsCache(HttpServletRequest request) {
        resetServerSettingsCache(getLogicsConnection(request));
    }
}
