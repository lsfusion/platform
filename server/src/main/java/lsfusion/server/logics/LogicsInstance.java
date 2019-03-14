package lsfusion.server.logics;

import com.google.common.base.Throwables;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.base.context.EventThreadInfo;
import lsfusion.server.base.context.LogicsInstanceContext;
import lsfusion.server.base.context.ThreadInfo;
import lsfusion.server.base.context.ThreadLocalContext;
import lsfusion.server.base.lifecycle.LifecycleManager;
import lsfusion.server.base.stack.NestedThreadException;
import lsfusion.server.base.stack.ThrowableWithStack;
import lsfusion.server.logics.navigator.NavigatorsManager;
import lsfusion.server.physics.admin.authentication.SecurityManager;
import lsfusion.server.physics.admin.reflection.ReflectionManager;
import lsfusion.server.physics.exec.DBManager;
import lsfusion.server.remote.RMIManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

public class LogicsInstance implements InitializingBean {
    private static final Logger logger = ServerLoggers.startLogger;

    private final LogicsInstanceContext context;

    private LifecycleManager lifecycle;
    
    private BusinessLogics businessLogics;

    private NavigatorsManager navigatorsManager;

    private RestartManager restartManager;

    private SecurityManager securityManager;

    private DBManager dbManager;

    private ReflectionManager reflectionManager;

    private RMIManager rmiManager;

    private Settings settings;

    private Map<Class, Object> customObjects;

    public LogicsInstance() {
        context = new LogicsInstanceContext(this);
    }

    public LogicsInstanceContext getContext() {
        return context;
    }

    public void setLifecycle(LifecycleManager lifecycle) {
        this.lifecycle = lifecycle;
    }

    public BusinessLogics getBusinessLogics() {
        return businessLogics;
    }

    public void setBusinessLogics(BusinessLogics businessLogics) {
        this.businessLogics = businessLogics;
    }

    public NavigatorsManager getNavigatorsManager() {
        return navigatorsManager;
    }

    public void setNavigatorsManager(NavigatorsManager navigatorsManager) {
        this.navigatorsManager = navigatorsManager;
    }

    public RestartManager getRestartManager() {
        return restartManager;
    }

    public void setRestartManager(RestartManager restartManager) {
        this.restartManager = restartManager;
    }

    public SecurityManager getSecurityManager() {
        return securityManager;
    }

    public void setSecurityManager(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    public DBManager getDbManager() {
        return dbManager;
    }

    public void setDbManager(DBManager dbManager) {
        this.dbManager = dbManager;
    }

    public ReflectionManager getReflectionManager() {
        return reflectionManager;
    }

    public void setReflectionManager(ReflectionManager reflectionManager) {
        this.reflectionManager = reflectionManager;
    }

    public RMIManager getRmiManager() {
        return rmiManager;
    }

    public void setRmiManager(RMIManager rmiManager) {
        this.rmiManager = rmiManager;
    }

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }
    
    public void setCustomObjects(Object... objects) {
        if (objects.length == 0) {
            customObjects = null;
            return;
        }

        customObjects = new HashMap<>();
        for (Object obj : objects) {
            if (obj == null) {
                throw new NullPointerException("Custom object can't be null");
            }
            customObjects.put(obj.getClass(), obj);
        }
    }

    public <T> T getCustomObject(Class<T> clazz) {
        return getCustomObject(clazz, false);
    }

    public <T> T getCustomObject(Class<T> clazz, boolean allowSubClasses) {
        if (customObjects == null) {
            return null;
        }
        if (!allowSubClasses) {
            return (T) customObjects.get(clazz);
        } else {
            for (Map.Entry<Class, Object> e : customObjects.entrySet()) {
                if (clazz.isAssignableFrom(e.getKey())) {
                    return (T) e.getValue();
                }
            }
        }
        return null;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(businessLogics, "businessLogics must be specified");
        Assert.notNull(navigatorsManager, "navigatorsManager must be specified");
        Assert.notNull(restartManager, "restartManager must be specified");
        Assert.notNull(securityManager, "securityManager must be specified");
        Assert.notNull(dbManager, "dbManager must be specified");
        Assert.notNull(settings, "settings must be specified");
    }

    public void start() {
        logger.info("Logics instance is starting...");
        try {
            ThreadInfo threadInfo = EventThreadInfo.START();
            ThreadLocalContext.aspectBeforeLifecycle(this, threadInfo);
            try {
                lifecycle.fireStarting();
                lifecycle.fireStarted();

                businessLogics.cleanCaches();
            } finally {
                ThreadLocalContext.aspectAfterLifecycle(threadInfo);
            }

            logger.info("Logics instance has successfully started");
        } catch (Throwable throwable) {
            ThrowableWithStack[] throwables = throwable instanceof NestedThreadException // don't need NestedThreadException message+stack (they are always the same / don't matter)
                                     ? ((NestedThreadException) throwable).getThrowables()
                                     : new ThrowableWithStack[]{new ThrowableWithStack(throwable)};
            
            for (ThrowableWithStack nestedThrowable : throwables) {
                nestedThrowable.log("Exception while starting logics instance", logger);
            }

            lifecycle.fireError();

            Throwables.propagate(throwable);
        }
    }

    public void stop() {
        logger.info("Logics instance is stopping...");
        lifecycle.fireStopping();
        lifecycle.fireStopped();
        logger.info("Logics instance has stopped...");
    }
}
