package lsfusion.http.provider.navigator;

import lsfusion.base.BaseUtils;
import lsfusion.base.ServerUtils;
import lsfusion.base.SystemUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.http.authentication.LSFAuthenticationToken;
import lsfusion.http.controller.ExternalLogicsAndSessionRequestHandler;
import lsfusion.http.controller.MainController;
import lsfusion.http.provider.SessionInvalidatedException;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.connection.ComputerInfo;
import lsfusion.interop.connection.ConnectionInfo;
import lsfusion.interop.connection.UserInfo;
import lsfusion.interop.logics.LogicsSessionObject;
import lsfusion.interop.logics.ServerSettings;
import lsfusion.interop.logics.remote.RemoteLogicsInterface;
import lsfusion.interop.navigator.NavigatorInfo;
import lsfusion.interop.navigator.remote.RemoteNavigatorInterface;
import lsfusion.interop.session.ExternalRequest;
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
import java.net.URLDecoder;
import java.rmi.RemoteException;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// session scoped - one for one browser (! not tab)
public class NavigatorProviderImpl implements NavigatorProvider, DisposableBean {

    public NavigatorProviderImpl() {}

    public String servSID = GwtSharedUtils.randomString(25);

    public static ConnectionInfo getConnectionInfo(Authentication auth) {
        Locale clientLocale = LocaleContextHolder.getLocale();
        return new ConnectionInfo(new ComputerInfo(SystemUtils.getLocalHostName(), ((WebAuthenticationDetails) auth.getDetails()).getRemoteAddress()), new UserInfo(clientLocale.getLanguage(), clientLocale.getCountry(), LocaleContextHolder.getTimeZone(), BaseUtils.getDatePattern(), BaseUtils.getTimePattern(), null));
    }

    public static SessionInfo getSessionInfo(HttpServletRequest request) {
        return new SessionInfo(getConnectionInfo(request), MainController.getExternalRequest(new ExternalRequest.Param[0], request));
    }

    public static ConnectionInfo getConnectionInfo(HttpServletRequest request) {
        Locale clientLocale = LocaleContextHolder.getLocale();

        String hostName = ExternalLogicsAndSessionRequestHandler.getRequestCookies(request).get(ServerUtils.HOSTNAME_COOKIE_NAME);
        if(hostName == null)
            hostName = request.getRemoteHost();

        Cookie timeZone = WebUtils.getCookie(request, "LSFUSION_CLIENT_TIME_ZONE");
        Cookie timeFormat = WebUtils.getCookie(request, "LSFUSION_CLIENT_TIME_FORMAT");
        Cookie dateFormat = WebUtils.getCookie(request, "LSFUSION_CLIENT_DATE_FORMAT");

        Cookie colorTheme = WebUtils.getCookie(request, "LSFUSION_CLIENT_COLOR_THEME");

        return new ConnectionInfo(new ComputerInfo(hostName, request.getRemoteAddr()), new UserInfo(clientLocale.getLanguage(), clientLocale.getCountry(), timeZone != null ? TimeZone.getTimeZone(URLDecoder.decode(timeZone.getValue())) : null, dateFormat != null ? URLDecoder.decode(dateFormat.getValue()) : null, timeFormat != null ? URLDecoder.decode(timeFormat.getValue()) : null, colorTheme != null ? colorTheme.getValue() : null));
    }

    private static NavigatorInfo getNavigatorInfo(HttpServletRequest request) {
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

//        we don't need client locale here, because it was already updated when authenticating
//        Locale clientLocale = LSFAuthenticationToken.getLocale(auth);
//        if(clientLocale == null)
//            clientLocale = Locale.getDefault(); // it's better to pass and use client locale here         
//        String language = clientLocale.getLanguage();
//        String country = clientLocale.getCountry();

        return new NavigatorInfo(getSessionInfo(request), osVersion, processor, architecture, cores, physicalMemory, totalMemory,
                maximumMemory, freeMemory, javaVersion, BaseUtils.getPlatformVersion(), BaseUtils.getApiVersion());
    }

    // required for correct Jasper report generation
    private RemoteLogicsInterface remoteLogics;

    @Override
    public RemoteLogicsInterface getRemoteLogics() {
        return remoteLogics;
    }

    @Override
    public String createNavigator(LogicsSessionObject sessionObject, HttpServletRequest request) throws RemoteException {
        this.remoteLogics = sessionObject.remoteLogics;
        String sessionID = nextSessionID();
        addLogicsAndNavigatorSessionObject(sessionID, createNavigatorSessionObject(sessionObject, request));
        return sessionID;
    }

    private NavigatorSessionObject createNavigatorSessionObject(LogicsSessionObject sessionObject, HttpServletRequest request) throws RemoteException {
        AuthenticationToken lsfToken = LSFAuthenticationToken.getAppServerToken();

        NavigatorInfo navigatorInfo = getNavigatorInfo(request);
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
            throw new SessionInvalidatedException("Navigator " + sessionID);
        return navigatorSessionObject;
    }

    @Override
    public NavigatorSessionObject createOrGetNavigatorSessionObject(String sessionID, LogicsSessionObject sessionObject, HttpServletRequest request) throws RemoteException {
        NavigatorSessionObject navigatorSessionObject = currentLogicsAndNavigators.get(sessionID);
        if(navigatorSessionObject == null) {
            navigatorSessionObject = createNavigatorSessionObject(sessionObject, request);
            addLogicsAndNavigatorSessionObject(sessionID, navigatorSessionObject);
        }
        return navigatorSessionObject;
    }

    @Override
    public void removeNavigatorSessionObject(String sessionID) throws RemoteException {
        NavigatorSessionObject navigatorSessionObject = getNavigatorSessionObject(sessionID);
        MainDispatchServlet.logger.error("Removing navigator " + sessionID + "...");
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
