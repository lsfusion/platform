package lsfusion.http.provider.navigator;

import lsfusion.base.BaseUtils;
import lsfusion.base.ServerUtils;
import lsfusion.base.SystemUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.navigator.ConnectionInfo;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.http.authentication.LSFAuthenticationToken;
import lsfusion.http.controller.ExternalLogicsAndSessionRequestHandler;
import lsfusion.http.controller.MainController;
import lsfusion.http.provider.SessionInvalidatedException;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.connection.ClientType;
import lsfusion.interop.logics.LogicsSessionObject;
import lsfusion.interop.logics.ServerSettings;
import lsfusion.interop.logics.remote.RemoteLogicsInterface;
import lsfusion.interop.navigator.NavigatorInfo;
import lsfusion.interop.navigator.remote.RemoteNavigatorInterface;
import lsfusion.interop.session.SessionInfo;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.web.util.WebUtils;
import ua_parser.Client;
import ua_parser.Parser;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.rmi.RemoteException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// session scoped - one for one browser (! not tab)
public class NavigatorProviderImpl implements NavigatorProvider, DisposableBean {

    public String servSID = GwtSharedUtils.randomString(25);
    
    public static SessionInfo getSessionInfo(Authentication auth, HttpServletRequest request) {
        Locale clientLocale = LocaleContextHolder.getLocale();
        return new SessionInfo(SystemUtils.getLocalHostName(), ((WebAuthenticationDetails) auth.getDetails()).getRemoteAddress(), clientLocale.getLanguage(), clientLocale.getCountry(),
                BaseUtils.getDatePattern(), BaseUtils.getTimePattern(), MainController.getExternalRequest(new Object[0], request));
    }

    public static SessionInfo getSessionInfo(HttpServletRequest request) {
        String hostName = ExternalLogicsAndSessionRequestHandler.getRequestCookies(request).get(ServerUtils.HOSTNAME_COOKIE_NAME);
        if(hostName == null)
            hostName = request.getRemoteHost();

        return new SessionInfo(hostName, request.getRemoteAddr(), null, null, null, null, // we don't need client language and country because they were already provided when authenticating (see method above)
                MainController.getExternalRequest(new Object[0], request));
    }

    private static NavigatorInfo getNavigatorInfo(HttpServletRequest request, ConnectionInfo connectionInfo) {
        String osVersion;
        String architecture = null;
        String processor = null;
        String userAgent = request.getHeader("User-Agent");
        if(userAgent != null) {
            Client c = new Parser().parse(userAgent);
            osVersion = c.os.family + (c.os.major != null ? (" " + c.os.major) : "");
        } else {
            osVersion = System.getProperty("os.name"); //server os
            architecture = System.getProperty("os.arch");
            if (osVersion.startsWith("Windows")) {
                String arch = System.getenv("PROCESSOR_ARCHITECTURE");
                String wow64Arch = System.getenv("PROCESSOR_ARCHITEW6432");
                architecture = arch.endsWith("64") || wow64Arch != null && wow64Arch.endsWith("64") ? "x64" : "x32";
            }
            processor = System.getenv("PROCESSOR_IDENTIFIER");
        }

        Integer cores = Runtime.getRuntime().availableProcessors();
        com.sun.management.OperatingSystemMXBean os = (com.sun.management.OperatingSystemMXBean)
                java.lang.management.ManagementFactory.getOperatingSystemMXBean();
        Integer physicalMemory = (int) (os.getTotalPhysicalMemorySize() / 1048576);
        Integer totalMemory = (int) (Runtime.getRuntime().totalMemory() / 1048576);
        Integer maximumMemory = (int) (Runtime.getRuntime().maxMemory() / 1048576);
        Integer freeMemory = (int) (Runtime.getRuntime().freeMemory() / 1048576);
        String javaVersion = SystemUtils.getJavaVersion() + " " + System.getProperty("sun.arch.data.model") + " bit";
        ClientType clientType = connectionInfo != null && connectionInfo.mobile ? ClientType.WEB_MOBILE : ClientType.WEB_DESKTOP;
        String screenSize = connectionInfo != null ? connectionInfo.screenSize : null;

//        we don't need client locale here, because it was already updated when authenticating
//        Locale clientLocale = LSFAuthenticationToken.getLocale(auth);
//        if(clientLocale == null)
//            clientLocale = Locale.getDefault(); // it's better to pass and use client locale here         
//        String language = clientLocale.getLanguage();
//        String country = clientLocale.getCountry();

        return new NavigatorInfo(getSessionInfo(request), osVersion, processor, architecture, cores, physicalMemory, totalMemory,
                maximumMemory, freeMemory, javaVersion, screenSize, clientType, BaseUtils.getPlatformVersion(), BaseUtils.getApiVersion());
    }

    // required for correct Jasper report generation
    private RemoteLogicsInterface remoteLogics;

    @Override
    public RemoteLogicsInterface getRemoteLogics() {
        return remoteLogics;
    }

    @Override
    public String createNavigator(LogicsSessionObject sessionObject, HttpServletRequest request, ConnectionInfo connectionInfo) throws RemoteException {
        this.remoteLogics = sessionObject.remoteLogics;
        String sessionID = nextSessionID();
        addLogicsAndNavigatorSessionObject(sessionID, createNavigatorSessionObject(sessionObject, request, connectionInfo));
        return sessionID;
    }

    private NavigatorSessionObject createNavigatorSessionObject(LogicsSessionObject sessionObject, HttpServletRequest request, ConnectionInfo connectionInfo) throws RemoteException {
        AuthenticationToken lsfToken = LSFAuthenticationToken.getAppServerToken();

        NavigatorInfo navigatorInfo = getNavigatorInfo(request, connectionInfo);
        RemoteNavigatorInterface remoteNavigator = sessionObject.remoteLogics.createNavigator(lsfToken, navigatorInfo);

        ServerSettings serverSettings = sessionObject.getServerSettings(navigatorInfo.session, null, false);
        if (serverSettings.sessionConfigTimeout > 0)
            request.getSession().setMaxInactiveInterval(serverSettings.sessionConfigTimeout);
        return new NavigatorSessionObject(remoteNavigator, serverSettings);
    }

    @Override
    public String getSessionInfo() {
        return "SESSION " + servSID + " CURRENT OPENED TABS " + currentLogicsAndNavigators.keySet();
    }

    private AtomicInteger nextSessionId = new AtomicInteger(0);
    private String nextSessionID() {
        return "session" + nextSessionId.getAndIncrement();
    }

    private final Map<String, NavigatorSessionObject> currentLogicsAndNavigators = new ConcurrentHashMap<>();

    private void addLogicsAndNavigatorSessionObject(String sessionID, NavigatorSessionObject navigatorSessionObject) {
        currentLogicsAndNavigators.put(sessionID, navigatorSessionObject);
    }

    @Override
    public NavigatorSessionObject getNavigatorSessionObject(String sessionID) throws SessionInvalidatedException {
        NavigatorSessionObject navigatorSessionObject = currentLogicsAndNavigators.get(sessionID);
        if(navigatorSessionObject == null)
            throw new SessionInvalidatedException();
        return navigatorSessionObject;
    }

    @Override
    public NavigatorSessionObject createOrGetNavigatorSessionObject(String sessionID, LogicsSessionObject sessionObject, HttpServletRequest request) throws RemoteException {
        NavigatorSessionObject navigatorSessionObject = currentLogicsAndNavigators.get(sessionID);
        if(navigatorSessionObject == null) {
            navigatorSessionObject = createNavigatorSessionObject(sessionObject, request, null);
            addLogicsAndNavigatorSessionObject(sessionID, navigatorSessionObject);
        }
        return navigatorSessionObject;
    }

    @Override
    public void removeNavigatorSessionObject(String sessionID) throws RemoteException {
        NavigatorSessionObject navigatorSessionObject = getNavigatorSessionObject(sessionID);
        currentLogicsAndNavigators.remove(sessionID);
        navigatorSessionObject.remoteNavigator.close();
    }

    @Override
    public ServerSettings getServerSettings(String sessionID) throws SessionInvalidatedException {
        return getNavigatorSessionObject(sessionID).serverSettings;
    }

    @Override
    public void destroy() throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        MainDispatchServlet.logger.error("Destroying navigator for user " + (auth == null ? "UNKNOWN" : auth.getName()) + "...");

        for(NavigatorSessionObject navigatorSessionObject : currentLogicsAndNavigators.values())
            navigatorSessionObject.remoteNavigator.close();
    }
}
