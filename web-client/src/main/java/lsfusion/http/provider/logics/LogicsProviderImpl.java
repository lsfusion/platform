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
    private static final String allowedHostsKey = "allowedHosts";

    private String host; // default host
    private int port; // default port
    private String exportName; // default export name

    private AllowedHosts allowedHosts = new AllowedHosts(""); // besides the default host; when not configured, the local subnets

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

        String allowedHosts = servletContext.getInitParameter(allowedHostsKey);
        String allowedHostsProperty = getSystemProperty("app.allowed.hosts");
        if (allowedHostsProperty != null)
            allowedHosts = allowedHostsProperty;

        if (host == null || port == null || exportName == null)
            throw new IllegalStateException(hostKey + " or " + portKey + " or " + exportNameKey + " parameters aren't set in web.xml");

        FileUtils.APP_CONTEXT_FOLDER_PATH = servletContext.getRealPath("");
        FileUtils.setTempFolder(((File) servletContext.getAttribute(ServletContext.TEMPDIR)).getPath()); // appPath + "/WEB-INF/temp";

        setHost(host);
        setPort(Integer.parseInt(port));
        setExportName(exportName);
        setAllowedHosts(allowedHosts != null ? new AllowedHosts(allowedHosts) : AllowedHosts.getLocalSubnets());
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

    public void setAllowedHosts(AllowedHosts allowedHosts) {
        this.allowedHosts = allowedHosts;
    }

    private boolean isAllowed(String host) {
        host = AllowedHosts.getCanonical(host);
        return host.equals(AllowedHosts.getCanonical(this.host)) || allowedHosts.contains(host);
    }

    @Override
    public <R> R runRequest(LogicsConnection connection, LogicsRunnable<R> runnable) throws AppServerNotAvailableException, RemoteException {
        if (connection.port < 1 || connection.port > 65535 || !isAllowed(connection.host))
            throw new AppServerNotAvailableException("Application server [" + connection.host + ":" + connection.port + "] is not allowed");
        return super.runRequest(connection, runnable);
    }

    // needed to recode exception; why an application server is not available goes to the log only
    private <R> R runRequestDispatch(LogicsConnection logicsConnection, LogicsRunnable<R> runnable) throws RemoteException, AppServerNotAvailableDispatchException {
        try {
            return runRequest(logicsConnection, runnable);
        } catch (AppServerNotAvailableException e) {
            logger.error(getLogLine(e.getMessage()));
            throw new AppServerNotAvailableDispatchException("Application server is not available");
        }
    }

    static String getLogLine(String message) { // the host and the export name come from the request
        message = message.replaceAll("[\\p{Cntrl}\\u0085\\u2028\\u2029]", "?");
        return message.length() > 500 ? message.substring(0, 497) + "..." : message;
    }

    private LogicsConnection getLogicsConnection(String host, Integer port, String exportName) {
        return new LogicsConnection(AllowedHosts.getCanonical(host != null ? host : this.host), port != null ? port : this.port, exportName != null ? exportName : this.exportName);
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
            return runRequestDispatch(getLogicsConnection(request), (sessionObject, retry) -> getServerSettings(request, noCache, sessionObject));
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
