package platform.gwt.base.server.spring;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;
import platform.base.OSUtils;
import platform.base.ReflectionUtils;
import platform.interop.RemoteLogicsInterface;
import platform.interop.navigator.RemoteNavigatorInterface;

import java.rmi.RemoteException;

public class NavigatorProviderImpl implements NavigatorProvider, InitializingBean, DisposableBean, InvalidateListener {
    @Autowired
    private BusinessLogicsProvider blProvider;

    private volatile RemoteNavigatorInterface navigator;
    private final Object navigatorLock = new Object();

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

                    try {
                        RemoteLogicsInterface bl = blProvider.getLogics();

                        RemoteNavigatorInterface unsynced = bl.createNavigator(true, username, password, bl.getComputer(OSUtils.getLocalHostName()), true);
                        if (unsynced == null) {
                            throw new IllegalStateException("Не могу создать навигатор.");
                        }
                        navigator = ReflectionUtils.makeSynchronized(RemoteNavigatorInterface.class, unsynced);
                    } catch (RemoteException e) {
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
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(blProvider, "businessLogicProvider must be specified");
        blProvider.addInvlidateListener(this);
    }

    @Override
    public void destroy() throws Exception {
        blProvider.removeInvlidateListener(this);
    }
}
