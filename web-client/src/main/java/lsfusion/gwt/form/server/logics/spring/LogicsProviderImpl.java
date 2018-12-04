package lsfusion.gwt.form.server.logics.spring;

import com.google.gwt.core.client.GWT;
import lsfusion.base.NavigatorInfo;
import lsfusion.base.SystemUtils;
import lsfusion.client.logics.ClientForm;
import lsfusion.client.logics.ClientFormChanges;
import lsfusion.client.serialization.ClientSerializationPool;
import lsfusion.gwt.base.server.spring.InvalidateListener;
import lsfusion.gwt.form.server.convert.ClientComponentToGwtConverter;
import lsfusion.gwt.form.server.convert.ClientFormChangesToGwtConverter;
import lsfusion.gwt.form.server.form.spring.FormProvider;
import lsfusion.gwt.form.server.form.spring.FormSessionObject;
import lsfusion.gwt.form.server.navigator.spring.NavigatorProvider;
import lsfusion.gwt.form.server.navigator.spring.NavigatorSessionObject;
import lsfusion.gwt.form.shared.view.GForm;
import lsfusion.gwt.form.shared.view.GFormUserPreferences;
import lsfusion.gwt.form.shared.view.GNavigator;
import lsfusion.interop.RemoteLogicsLoaderInterface;
import lsfusion.interop.action.FormClientAction;
import lsfusion.interop.form.FormUserPreferences;
import lsfusion.interop.form.RemoteFormInterface;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.interop.remote.RMIUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import lsfusion.interop.RemoteLogicsInterface;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.ServletContext;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static lsfusion.base.BaseUtils.nvl;

// singleton, one for whole application
public class LogicsProviderImpl<T extends RemoteLogicsInterface> implements InitializingBean, LogicsProvider<T> {

    protected final static Logger logger = Logger.getLogger(LogicsProviderImpl.class);

    private String registryHostKey = "registryHost";
    private String registryPortKey = "registryPort";
    private String exportNameKey = "exportName";

    private String registryHost;
    private int registryPort;
    private String exportName;

    public LogicsProviderImpl() {
    }

    @Autowired
    private ServletContext servletContext;

    @Override
    public void afterPropertiesSet() throws Exception {
        String registryHost = servletContext.getInitParameter(registryHostKey);
        String registryPort = servletContext.getInitParameter(registryPortKey);
        String exportName = nvl(servletContext.getInitParameter(exportNameKey), "default");
        if (registryHost == null || registryPort == null) {
            throw new IllegalStateException(registryHostKey + " or " + registryPortKey + " parameters aren't set in web.xml");
        }

        setRegistryHost(registryHost);
        setRegistryPort(Integer.parseInt(registryPort));
        setExportName(exportName);
    }

    public void setRegistryHostKey(String registryHostKey) {
        this.registryHostKey = registryHostKey;
    }

    public void setRegistryPortKey(String registryPortKey) {
        this.registryPortKey = registryPortKey;
    }

    public void setExportNameKey(String exportNameKey) {
        this.exportNameKey = exportNameKey;
    }

    public String getRegistryHost() {
        return registryHost;
    }

    public void setRegistryHost(String registryHost) {
        this.registryHost = registryHost;
    }

    public int getRegistryPort() {
        return registryPort;
    }

    public void setRegistryPort(int registryPort) {
        this.registryPort = registryPort;
    }

    public String getExportName() {
        return exportName;
    }

    public void setExportName(String exportName) {
        this.exportName = exportName;
    }

    private volatile T logics;

    public T getLogics() throws RemoteException {
        readLogicsLock.lock();

        //double-check locking
        if (logics == null) {
            readLogicsLock.unlock();

            writeLogicsLock.lock();
            try {
                if (logics == null) {
                    createRemoteLogics();
                }
                return logics;
            } finally {
                writeLogicsLock.unlock();
            }
        }
        try {
            return logics;
        } finally {
            readLogicsLock.unlock();
        }
    }

    private final ReadWriteLock logicsLock = new ReentrantReadWriteLock();
    private final Lock readLogicsLock = logicsLock.readLock();
    private final Lock writeLogicsLock = logicsLock.writeLock();

    private void createRemoteLogics() throws RemoteException {
        try {
            RemoteLogicsLoaderInterface loader = RMIUtils.rmiLookup(registryHost, registryPort, exportName, "RemoteLogicsLoader");

            logics = (T) loader.getLogics();
        } catch (NotBoundException | MalformedURLException e) {
            logger.error("Ошибка при получении объекта логики: ", e);
            throw new RuntimeException("Произошла ошибка при подлючении к серверу приложения.", e);
        }
    }

    private final List<InvalidateListener> invalidateListeners = Collections.synchronizedList(new ArrayList<InvalidateListener>());

    @Override
    public void invalidate() {
        try {
            GWT.log("Invalidating logics...", new Exception());
        } catch (Throwable ignored) {} // валится при попытке подключиться после перестарта сервера

        writeLogicsLock.lock();
        try {
            logics = null;

            for (InvalidateListener invalidateListener : invalidateListeners) {
                invalidateListener.onInvalidate();
            }
        } finally {
            writeLogicsLock.unlock();
        }
    }

    @Override
    public void addInvalidateListener(InvalidateListener listener) {
        invalidateListeners.add(listener);
    }

    @Override
    public void removeInvalidateListener(InvalidateListener listener) {
        invalidateListeners.remove(listener);
    }

    public GNavigator createNavigator(String logicsID, LogicsSessionObject logicsSessionObject, NavigatorProvider navigatorProvider) throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
//            auth = new TestingAuthenticationToken("admin", "fusion");
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

        RemoteLogicsInterface bl = logicsSessionObject.remoteLogics;
/*                        RemoteNavigatorInterface unsynced = bl.createNavigator(true, new NavigatorInfo(username, password,
                bl.getComputer(SystemUtils.getLocalHostName()), ((WebAuthenticationDetails) auth.getDetails()).getRemoteAddress(),
                osVersion, processor, architecture, cores, physicalMemory, totalMemory, maximumMemory, freeMemory,
                javaVersion, null, language, country), true);*/
        RemoteNavigatorInterface remoteNavigator = bl.createNavigator(true, new NavigatorInfo(username, password,
                bl.getComputer(SystemUtils.getLocalHostName()), "127.0.0.1", osVersion, processor, architecture,
                cores, physicalMemory, totalMemory, maximumMemory, freeMemory, javaVersion, null, language, country), true);

        GNavigator gNavigator = new GNavigator();
        gNavigator.sessionID = navigatorProvider.addNavigatorSessionObject(new NavigatorSessionObject(remoteNavigator, logicsID));
        return gNavigator;
    }
}
