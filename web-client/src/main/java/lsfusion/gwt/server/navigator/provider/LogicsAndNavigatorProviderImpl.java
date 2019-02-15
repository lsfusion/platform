

package lsfusion.gwt.server.navigator.provider;

import com.google.gwt.core.client.GWT;
import lsfusion.base.NavigatorInfo;
import lsfusion.base.SessionInfo;
import lsfusion.base.SystemUtils;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.shared.GwtSharedUtils;
import lsfusion.http.LSFAuthenticationToken;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.interop.remote.AuthenticationToken;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import javax.servlet.http.HttpServletRequest;
import java.rmi.RemoteException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// session scoped - one for one browser (! not tab)
public class LogicsAndNavigatorProviderImpl implements LogicsAndNavigatorProvider, DisposableBean {

    public String servSID = GwtSharedUtils.randomString(25);
    
    public static SessionInfo getSessionInfo(Authentication auth) {
        Locale clientLocale = LocaleContextHolder.getLocale();
        return new SessionInfo(SystemUtils.getLocalHostName(), ((WebAuthenticationDetails) auth.getDetails()).getRemoteAddress(), clientLocale.getLanguage(), clientLocale.getCountry());
    }

    public static SessionInfo getSessionInfo(HttpServletRequest request) {
        return new SessionInfo(request.getRemoteHost(), request.getRemoteHost(), null, null); // we don't need client language and country because they were already provided when authenticating (see method above)
    }

    private static NavigatorInfo getNavigatorInfo(HttpServletRequest request) {
        String osVersion = System.getProperty("os.name");
        String processor = System.getenv("PROCESSOR_IDENTIFIER");

        String architecture = System.getProperty("os.arch");
        if (osVersion.startsWith("Windows")) {
            String arch = System.getenv("PROCESSOR_ARCHITECTURE");
            String wow64Arch = System.getenv("PROCESSOR_ARCHITEW6432");
            architecture = arch.endsWith("64") || wow64Arch != null && wow64Arch.endsWith("64") ? "x64" : "x32";
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

        return new NavigatorInfo(getSessionInfo(request), osVersion, processor, architecture, cores, physicalMemory, totalMemory, maximumMemory, freeMemory, javaVersion, null);
    }

    public String createNavigator(RemoteLogicsInterface remoteLogics, MainDispatchServlet servlet) throws RemoteException {
        AuthenticationToken lsfToken = LSFAuthenticationToken.getAppServerToken();

        RemoteNavigatorInterface remoteNavigator = remoteLogics.createNavigator(lsfToken, getNavigatorInfo(servlet.getRequest()));

        return addLogicsAndNavigatorSessionObject(new LogicsAndNavigatorSessionObject(remoteLogics, remoteNavigator));
    }

    @Override
    public String getSessionInfo() {
        return "SESSION " + servSID + " CURRENT OPENED TABS " + currentLogicsAndNavigators.keySet();
    }

    private final Map<String, LogicsAndNavigatorSessionObject> currentLogicsAndNavigators = new ConcurrentHashMap<>();

    private AtomicInteger nextSessionId = new AtomicInteger(0);
    private String nextSessionID() {
        return "session" + nextSessionId.getAndIncrement();
    }
    private String addLogicsAndNavigatorSessionObject(LogicsAndNavigatorSessionObject logicsAndNavigatorSessionObject) {
        String sessionID = nextSessionID();
        currentLogicsAndNavigators.put(sessionID, logicsAndNavigatorSessionObject);
        return sessionID;
    }

    @Override
    public LogicsAndNavigatorSessionObject getLogicsAndNavigatorSessionObject(String sessionID) {
        return currentLogicsAndNavigators.get(sessionID);
    }

    @Override
    public void removeLogicsAndNavigatorSessionObject(String sessionID) throws RemoteException {
        LogicsAndNavigatorSessionObject logicsAndNavigatorSessionObject = currentLogicsAndNavigators.remove(sessionID);
        logicsAndNavigatorSessionObject.remoteNavigator.close();
    }

    @Override
    public void destroy() throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        GWT.log("Destroying navigator for user " + (auth == null ? "UNKNOWN" : auth.getName()) + "...", new Exception());

        for(LogicsAndNavigatorSessionObject logicsAndNavigatorSessionObject : currentLogicsAndNavigators.values())
            logicsAndNavigatorSessionObject.remoteNavigator.close();
    }
}
