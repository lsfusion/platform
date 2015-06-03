package lsfusion.gwt.base.server.spring;

import lsfusion.base.NavigatorInfo;
import lsfusion.base.ReflectionUtils;
import lsfusion.base.SystemUtils;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.rmi.RemoteException;

public class NavigatorProviderImpl implements NavigatorProvider, DisposableBean, InvalidateListener {

    private BusinessLogicsProvider blProvider;

    private volatile RemoteNavigatorInterface navigator;
    private final Object navigatorLock = new Object();

    public NavigatorProviderImpl(BusinessLogicsProvider blProvider) {
        this.blProvider = blProvider;
        blProvider.addInvalidateListener(this);
    }

    @Override
    public RemoteNavigatorInterface getNavigator() {
        //double-check locking
        if (navigator == null) {
            synchronized (navigatorLock) {
                if (navigator == null) {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    if (auth == null) {
//                        auth = new TestingAuthenticationToken("admin", "fusion");
                        throw new IllegalStateException("Пользователь должен быть аутентифицирован, чтобы использовать навигатор.");
                    }

                    String username = auth.getName();
                    String password = (String) auth.getCredentials();
                    String osVersion = System.getProperty("os.name");
                    String processor = System.getenv("PROCESSOR_IDENTIFIER");
                    String architecture = System.getenv("PROCESSOR_ARCHITECTURE");
                    Integer cores = Runtime.getRuntime().availableProcessors();
                    com.sun.management.OperatingSystemMXBean os = (com.sun.management.OperatingSystemMXBean)
                            java.lang.management.ManagementFactory.getOperatingSystemMXBean();
                    Integer physicalMemory = (int) (os.getTotalPhysicalMemorySize() / 1048576);
                    Integer totalMemory = (int) (Runtime.getRuntime().totalMemory() / 1048576);
                    Integer maximumMemory = (int) (Runtime.getRuntime().maxMemory() / 1048576);
                    Integer freeMemory = (int) (Runtime.getRuntime().freeMemory() / 1048576);
                    String javaVersion = System.getProperty("java.version") + " " + System.getProperty("sun.arch.data.model") + " bit";

                    try {
                        RemoteLogicsInterface bl = blProvider.getLogics();
                        RemoteNavigatorInterface unsynced = bl.createNavigator(true, new NavigatorInfo(username, password,
                                bl.getComputer(SystemUtils.getLocalHostName()), ((WebAuthenticationDetails) auth.getDetails()).getRemoteAddress(),
                                osVersion, processor, architecture, cores, physicalMemory, totalMemory, maximumMemory, freeMemory,
                                javaVersion, null), true);
//                        RemoteNavigatorInterface unsynced = bl.createNavigator(true, username, password, bl.getComputer(SystemUtils.getLocalHostName()), "127.0.0.1", osVersion + memory, javaVersion, true);
                        if (unsynced == null) {
                            throw new IllegalStateException("Не могу создать навигатор.");
                        }
                        navigator = ReflectionUtils.makeSynchronized(RemoteNavigatorInterface.class, unsynced);
                    } catch (RemoteException e) {
                        blProvider.invalidate();
                        throw new RuntimeException("Не могу создать навигатор.", e);
                    }
                }
            }
        }

        return navigator;
    }

    public void onInvalidate() {
        synchronized (navigatorLock) {
            navigator = null;
        }
    }

    @Override
    public void destroy() throws Exception {
        blProvider.removeInvalidateListener(this);

        RemoteNavigatorInterface navigator = getNavigator();
        if (navigator != null) {
            navigator.close();
        }
    }
}
