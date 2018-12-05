package lsfusion.gwt.form.server;

import com.google.common.base.Throwables;
import lsfusion.base.NavigatorInfo;
import lsfusion.base.SystemUtils;
import lsfusion.gwt.form.server.logics.spring.LogicsHandlerProvider;
import lsfusion.gwt.form.server.navigator.spring.LogicsAndNavigatorProvider;
import lsfusion.gwt.form.server.navigator.spring.LogicsAndNavigatorSessionObject;
import lsfusion.gwt.form.server.spring.LSFusionDispatchServlet;
import lsfusion.gwt.form.shared.actions.LookupLogicsAndCreateNavigator;
import lsfusion.gwt.form.shared.view.GNavigator;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import net.customware.gwt.dispatch.shared.general.StringResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Locale;

public class LookupLogicsAndCreateNavigatorHandler extends lsfusion.gwt.base.server.dispatch.SimpleActionHandlerEx<LookupLogicsAndCreateNavigator, StringResult, RemoteLogicsInterface> {

    public LookupLogicsAndCreateNavigatorHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public StringResult executeEx(LookupLogicsAndCreateNavigator action, ExecutionContext context) throws DispatchException, IOException {
        LogicsHandlerProvider logicsHandlerProvider = servlet.getLogicsHandlerProvider();
        RemoteLogicsInterface remoteLogics = logicsHandlerProvider.getLogics(action.host, action.port, action.exportName);
        try {
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

/*                        RemoteNavigatorInterface unsynced = bl.createNavigator(true, new NavigatorInfo(username, password,
                bl.getComputer(SystemUtils.getLocalHostName()), ((WebAuthenticationDetails) auth.getDetails()).getRemoteAddress(),
                osVersion, processor, architecture, cores, physicalMemory, totalMemory, maximumMemory, freeMemory,
                javaVersion, null, language, country), true);*/
            RemoteNavigatorInterface remoteNavigator = remoteLogics.createNavigator(true, new NavigatorInfo(username, password,
                    remoteLogics.getComputer(SystemUtils.getLocalHostName()), "127.0.0.1", osVersion, processor, architecture,
                    cores, physicalMemory, totalMemory, maximumMemory, freeMemory, javaVersion, null, language, country), true);

            String sessionID = servlet.getLogicsAndNavigatorProvider().addLogicsAndNavigatorSessionObject(new LogicsAndNavigatorSessionObject(remoteLogics, remoteNavigator));
            return new StringResult(sessionID);
        } catch (RemoteException e) {
            logicsHandlerProvider.invalidate();
            throw Throwables.propagate(e);
        }
    }
}
