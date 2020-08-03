package lsfusion.server.logics;

import com.google.common.base.Throwables;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.server.base.controller.manager.LifecycleManager;
import lsfusion.server.base.controller.remote.RmiManager;
import lsfusion.server.base.controller.stack.NestedThreadException;
import lsfusion.server.base.controller.stack.ThrowableWithStack;
import lsfusion.server.base.controller.thread.EventThreadInfo;
import lsfusion.server.base.controller.thread.ExecutorFactoryThreadInfo;
import lsfusion.server.base.controller.thread.ThreadInfo;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.logics.controller.manager.RestartManager;
import lsfusion.server.logics.navigator.controller.manager.NavigatorsManager;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.authentication.security.controller.manager.SecurityManager;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.admin.reflection.controller.manager.ReflectionManager;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

public class LogicsInstance implements InitializingBean {
    private static final Logger logger = ServerLoggers.startLogger;
    protected final static Logger lruLogger = ServerLoggers.lruLogger;

    private final LogicsInstanceContext context;

    private LifecycleManager lifecycle;
    
    private BusinessLogics businessLogics;

    private NavigatorsManager navigatorsManager;

    private RestartManager restartManager;

    private SecurityManager securityManager;

    private DBManager dbManager;

    private ReflectionManager reflectionManager;

    private RmiManager rmiManager;

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

    public RmiManager getRmiManager() {
        return rmiManager;
    }

    public void setRmiManager(RmiManager rmiManager) {
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
    public void afterPropertiesSet() {
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
            Runnable beforeAspect = () -> ThreadLocalContext.aspectBeforeLifecycle(this, threadInfo);
            Runnable afterAspect = () -> ThreadLocalContext.aspectAfterLifecycle(threadInfo);
            beforeAspect.run();
            try {
                LRUUtil.initLRUTuner(lruLogger::info, beforeAspect, afterAspect,
                        () -> ((double)Settings.get().getTargetLRURangePercent() / 100.0), 
                        () -> ((double)Settings.get().getCriticalLRURangePercent() / 100.0),
                        () -> Settings.get().getTargetLRUAdjustIncCoeff(),
                        () -> Settings.get().getTargetLRUAdjustDecCoeff(),
                        () -> Settings.get().getCriticalLRUAdjustCoeff(),
                        Settings.get().getLRURangeDefaultCoeff(),
                        () -> Settings.get().getLRURangeMinCoeff(),
                        () -> Settings.get().getLRURangeMaxCoeff(),
                        () -> Settings.get().getStableLRUMinCount(),
                        () -> Settings.get().getUnstableLRUMaxCount());

                lifecycle.fireStarting();
                lifecycle.fireStarted();

                businessLogics.cleanCaches();
            } finally {
                afterAspect.run();
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
