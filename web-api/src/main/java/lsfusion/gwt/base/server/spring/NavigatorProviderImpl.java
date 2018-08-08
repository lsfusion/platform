package lsfusion.gwt.base.server.spring;

import com.google.common.base.Throwables;
import com.google.gwt.core.client.GWT;
import lsfusion.base.NavigatorInfo;
import lsfusion.base.ReflectionUtils;
import lsfusion.base.SystemUtils;
import lsfusion.gwt.base.shared.GwtSharedUtils;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.interop.remote.ClientCallBackInterface;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class NavigatorProviderImpl implements NavigatorProvider, DisposableBean, InvalidateListener {

    private BusinessLogicsProvider blProvider;

    private volatile RemoteNavigatorInterface navigator;
    private final Object navigatorLock = new Object();

    public NavigatorProviderImpl(BusinessLogicsProvider blProvider) {
        this.blProvider = blProvider;
        blProvider.addInvalidateListener(this);
    }

    public String servSID = GwtSharedUtils.randomString(25);

    private ClientCallBackInterface clientCallBack;

    public final Set<String> openTabs = new HashSet<String>();

    @Override
    public ClientCallBackInterface getClientCallBack() throws RemoteException {
        if(clientCallBack == null)
            clientCallBack = getNavigator().getClientCallBack();
        return clientCallBack;
    }

    @Override
    public void tabOpened(String tabSID) {
        synchronized (openTabs) {
            openTabs.add(tabSID);
        }
    }

    @Override
    public synchronized boolean tabClosed(String tabSID) {
        synchronized (openTabs) {
            openTabs.remove(tabSID);
            return openTabs.isEmpty();
        }
    }

    @Override
    public String getSessionInfo() {
        synchronized (openTabs) {
            return "SESSION " + servSID + " CURRENT OPENED TABS " + openTabs;
        }
    }

    @Override
    public RemoteNavigatorInterface getNavigator() throws RemoteException {
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
                    
                    try {
                        RemoteLogicsInterface bl = blProvider.getLogics();
                        RemoteNavigatorInterface unsynced = bl.createNavigator(true, new NavigatorInfo(username, password,
                                bl.getComputer(SystemUtils.getLocalHostName()), ((WebAuthenticationDetails) auth.getDetails()).getRemoteAddress(),
                                osVersion, processor, architecture, cores, physicalMemory, totalMemory, maximumMemory, freeMemory,
                                javaVersion, null, language, country), true);
//                        RemoteNavigatorInterface unsynced = bl.createNavigator(true, new NavigatorInfo(username, password,
//                                bl.getComputer(SystemUtils.getLocalHostName()), "127.0.0.1", osVersion, processor, architecture,
//                                cores, physicalMemory, totalMemory, maximumMemory, freeMemory, javaVersion, null, language, country), true);
                        navigator = unsynced; // ReflectionUtils.makeSynchronized(RemoteNavigatorInterface.class, unsynced) - в десктопе не синхронизировалось, непонятно зачем здесь синхронизировать
                    } catch (RemoteException e) {
                        blProvider.invalidate();
                        throw e;
                    }
                }
            }
        }

        return navigator;
    }

    public void onInvalidate() {
        // пока не null'им навигатор, а то потом он собирается через unreferenced и убивает все свои формы
//        invalidate();
    }
    
    public void invalidate() {
        synchronized (openTabs) {
            assert openTabs.isEmpty();
        }
        synchronized (navigatorLock) {
            navigator = null;
        }
        clientCallBack = null;
    }

    @Override
    public void destroy() throws Exception {
        blProvider.removeInvalidateListener(this);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        GWT.log("Destroying navigator for user " + (auth == null ? "UNKNOWN" : auth.getName()) + "...", new Exception());

        RemoteNavigatorInterface navigator = getNavigator();
        if (navigator != null) {
            navigator.close();
        }
    }
}
