package lsfusion.gwt.form.server.navigator.spring;

import com.google.gwt.core.client.GWT;
import lsfusion.base.NavigatorInfo;
import lsfusion.base.SystemUtils;
import lsfusion.gwt.base.shared.GwtSharedUtils;
import lsfusion.gwt.form.server.form.spring.FormSessionObject;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.interop.remote.ClientCallBackInterface;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.synchronizedMap;

// session scoped - one for one browser (! not tab)
public class LogicsAndNavigatorProviderImpl implements LogicsAndNavigatorProvider, DisposableBean {

    public String servSID = GwtSharedUtils.randomString(25);

    public String createNavigator(RemoteLogicsInterface remoteLogics) throws RemoteException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            auth = new TestingAuthenticationToken("admin", "fusion");
//            throw new IllegalStateException("Пользователь должен быть аутентифицирован, чтобы использовать навигатор.");
        }

        String username = auth.getName();
        String password = (String) auth.getCredentials();
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

        String language = Locale.getDefault().getLanguage();
        String country = Locale.getDefault().getCountry();

/*                        RemoteNavigatorInterface unsynced = bl.createNavigator(true, new NavigatorInfo(username, password,
                bl.getComputer(SystemUtils.getLocalHostName()), ((WebAuthenticationDetails) auth.getDetails()).getRemoteAddress(),
                osVersion, processor, architecture, cores, physicalMemory, totalMemory, maximumMemory, freeMemory,
                javaVersion, null, language, country), true);*/
        RemoteNavigatorInterface remoteNavigator = remoteLogics.createNavigator(true, new NavigatorInfo(username, password,
                remoteLogics.getComputer(SystemUtils.getLocalHostName()), "127.0.0.1", osVersion, processor, architecture,
                cores, physicalMemory, totalMemory, maximumMemory, freeMemory, javaVersion, null, language, country), true);

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
