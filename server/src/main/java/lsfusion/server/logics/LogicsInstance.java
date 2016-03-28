package lsfusion.server.logics;

import com.google.common.base.Throwables;
import lsfusion.base.GlobalObject;
import lsfusion.base.MultiCauseException;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.context.LogicsInstanceContext;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.lifecycle.LifecycleManager;
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

    public MAddExclMap<Object, Object> twins = MapFact.mAddExclMap();
    public synchronized <T extends GlobalObject> T twinObject(T object) {
        T twin = (T) twins.get(object);
        if(twin!=null)
            return twin;

        twins.exclAdd(object, object);
        return object;
    }

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

        customObjects = new HashMap<Class, Object>();
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

        //rmiManager можно не проставлять, если не нужен ремоутинг, тогда при любом обращении к нему просто упадёт NPE
//        Assert.notNull(rmiManager, "dbManager must be specified");

        if (lifecycle == null) {
            lifecycle = new LifecycleManager();
        }

        //add default lifecycle listeners
        lifecycle.addLifecycleListeners(businessLogics, securityManager, dbManager, navigatorsManager);
    }

    public void start() {
        logger.info("Logics instance is starting...");
        try {
            ThreadLocalContext.set(context);
            lifecycle.fireStarting();
            lifecycle.fireStarted();
            
            businessLogics.cleanCaches();
            
            logger.info("Logics instance has successfully started");
        } catch (Throwable throwable) {
            Throwable[] throwables = throwable instanceof MultiCauseException
                                     ? ((MultiCauseException) throwable).getCauses()
                                     : new Throwable[]{throwable};
            
            String errorString = "";
            for (Throwable t : throwables) {
                errorString += t.getMessage();

                try {
                    throw t;
                } catch (ScriptParsingException parsingEx) {
                    // используем .info, т.к. это контролируемое сообщение для пользователя, а не системная ошибка
                    logger.info("Parsing error, while starting logics instance: \n" + parsingEx.getMessage());
                } catch (Throwable otherEx) {
                    logger.error("Exception while starting business logic: ", otherEx);
                }
            }

            lifecycle.fireError(errorString);

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
