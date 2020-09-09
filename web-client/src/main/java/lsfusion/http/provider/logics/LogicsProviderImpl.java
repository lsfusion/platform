package lsfusion.http.provider.logics;

import lsfusion.base.BaseUtils;
import lsfusion.client.controller.remote.proxy.RemoteLogicsLoaderProxy;
import lsfusion.gwt.client.base.exception.AppServerNotAvailableDispatchException;
import lsfusion.gwt.server.FileUtils;
import lsfusion.http.provider.navigator.NavigatorProviderImpl;
import lsfusion.interop.base.exception.AppServerNotAvailableException;
import lsfusion.interop.logics.AbstractLogicsProviderImpl;
import lsfusion.interop.logics.LogicsConnection;
import lsfusion.interop.logics.LogicsRunnable;
import lsfusion.interop.logics.ServerSettings;
import lsfusion.interop.logics.remote.RemoteLogicsLoaderInterface;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

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

    public LogicsProviderImpl() {
    }

    @Autowired
    private ServletContext servletContext;

    @Override
    public void afterPropertiesSet() throws Exception {
        String host = servletContext.getInitParameter(hostKey);
        String port = servletContext.getInitParameter(portKey);
        String exportName = servletContext.getInitParameter(exportNameKey);
        if (host == null || port == null || exportName == null) {
            throw new IllegalStateException(hostKey + " or " + portKey + " or " + exportNameKey + " parameters aren't set in web.xml");
        }

        String appPath = servletContext.getRealPath("");
        FileUtils.APP_IMAGES_FOLDER_URL = appPath + "/static/images/";
        FileUtils.APP_CSS_FOLDER_URL = appPath + "/static/css/";
        FileUtils.APP_CLIENT_IMAGES_FOLDER_URL = appPath + "/main/static/images/";
        FileUtils.APP_TEMP_FOLDER_URL = appPath + "/WEB-INF/temp";
        FileUtils.APP_PATH = appPath;

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

    public ServerSettings getServerSettings(final HttpServletRequest request, boolean noCache) {
        return getServerSettings(getLogicsConnection(request), NavigatorProviderImpl.getSessionInfo(request), request.getContextPath(), noCache);
    }

    public <R> R runRequest(String host, Integer port, String exportName, LogicsRunnable<R> runnable) throws RemoteException, AppServerNotAvailableDispatchException {
        return runRequestDispatch(getLogicsConnection(host, port, exportName), runnable);
    }

    public <R> R runRequest(HttpServletRequest request, LogicsRunnable<R> runnable) throws RemoteException, AppServerNotAvailableDispatchException {
        return runRequestDispatch(getLogicsConnection(request), runnable);
    }

    @Override
    protected RemoteLogicsLoaderInterface lookupLoader(LogicsConnection connection) throws RemoteException, NotBoundException, MalformedURLException {
        return new RemoteLogicsLoaderProxy(super.lookupLoader(connection), connection.host);
    }
}
