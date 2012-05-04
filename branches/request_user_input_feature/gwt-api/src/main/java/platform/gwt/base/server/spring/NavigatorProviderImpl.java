package platform.gwt.base.server.spring;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import platform.base.OSUtils;
import platform.gwt.base.server.FormSessionObject;
import platform.interop.RemoteLogicsInterface;
import platform.interop.navigator.RemoteNavigatorInterface;

import javax.servlet.http.HttpSession;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public class NavigatorProviderImpl<T extends RemoteLogicsInterface> implements NavigatorProvider<T>, InitializingBean {
    private BusinessLogicsProvider<T> blProvider;

    private final Object remoteLock = new Object();

    private volatile T bl;
    private volatile RemoteNavigatorInterface navigator;

    @Override
    public RemoteNavigatorInterface getNavigator() {
        //double-check locking
        if (needCreateNavigator()) {
            synchronized (remoteLock) {
                if (needCreateNavigator()) {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    if (auth == null) {
                        throw new IllegalStateException("Пользователь должен быть аутентифицирован, чтобы использовать навигатор.");
                    }

                    String username = auth.getName();
                    String password = (String) auth.getCredentials();

                    try {
                        cleanSessionForms();
                        bl = blProvider.getLogics();
                        navigator = bl.createNavigator(username, password, bl.getComputer(OSUtils.getLocalHostName()), true);
                        if (navigator == null) {
                            throw new IllegalStateException("Не могу создать навигатор.");
                        }
                    } catch (RemoteException e) {
                        throw new RuntimeException("Не могу создать навигатор.", e);
                    }
                }
            }
        }

        return navigator;
    }

    private void cleanSessionForms() {
        if (navigator != null) {
            RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
            if (attributes instanceof ServletRequestAttributes) {
                ServletRequestAttributes requestAttributes = (ServletRequestAttributes) attributes;
                HttpSession session = requestAttributes.getRequest().getSession(false);
                if (session != null) {

                    Set<String> keysToRemove = new HashSet<String>();

                    Enumeration enumer = session.getAttributeNames();
                    while (enumer.hasMoreElements()) {
                        String key = (String) enumer.nextElement();
                        if (session.getAttribute(key) instanceof FormSessionObject) {
                            keysToRemove.add(key);
                        }
                    }

                    for (String key : keysToRemove) {
                        session.removeAttribute(key);
                    }
                }
            }
        }
    }

    private boolean needCreateNavigator() {
        return navigator == null || bl != blProvider.getLogics();
    }

    @Override
    public void setBusinessLogicsProvider(BusinessLogicsProvider<T> businesslogicsProvider) {
        this.blProvider = businesslogicsProvider;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(blProvider, "businessLogicProvider must be specified");
    }
}
