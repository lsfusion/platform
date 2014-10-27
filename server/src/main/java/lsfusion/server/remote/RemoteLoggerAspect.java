package lsfusion.server.remote;

import lsfusion.base.BaseUtils;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.form.navigator.RemoteNavigator;
import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
public class RemoteLoggerAspect {
    private final static Logger logger = ServerLoggers.remoteLogger;

    private static Map<Integer, Boolean> remoteLoggerDebugEnabled = new ConcurrentHashMap<Integer, Boolean>();

    @Around("(execution(* lsfusion.interop.RemoteLogicsInterface.*(..))" +
            " || execution(* lsfusion.interop.form.RemoteFormInterface.*(..))" +
            " || execution(* lsfusion.interop.navigator.RemoteNavigatorInterface.*(..)))" +
            " && !execution(* *.ping(..))" +
            "&& target(target)")
    public Object executeRemoteMethod(ProceedingJoinPoint thisJoinPoint, Object target) throws Throwable {
        Integer user;
        if (target instanceof RemoteLogics) {
            user = ((RemoteLogics) target).getCurrentUser();
        } else if (target instanceof RemoteForm) {
            user = ((RemoteForm) target).getCurrentUser();
        } else {
            user = (Integer) ((RemoteNavigator) target).getUser().object;
        }
        long startTime = System.currentTimeMillis();
        Object result = thisJoinPoint.proceed();
        long runTime = System.currentTimeMillis() - startTime;
        
        boolean debugEnabled = user != null && isRemoteLoggerDebugEnabled(user);

        if (debugEnabled) {
            logger.debug(logCall(thisJoinPoint, runTime));
        } else {
            if(runTime > Settings.get().getRemoteLogTime())
                logger.info(logCall(thisJoinPoint, runTime));
        }

        return result;
    }

    private String logCall(ProceedingJoinPoint thisJoinPoint, long runTime) {
        return String.format(
                "Executing remote method (time: %1$d ms.): %2$s(%3$s)",
                runTime,
                thisJoinPoint.getSignature().getName(),
                BaseUtils.toString(", ", thisJoinPoint.getArgs())
        );
    }

    public static void setRemoteLoggerDebugEnabled(Integer user, Boolean enabled) {
        remoteLoggerDebugEnabled.put(user, enabled != null && enabled);
    }

    public boolean isRemoteLoggerDebugEnabled(Integer user) {
        Boolean lde = remoteLoggerDebugEnabled.get(user);
        return lde != null && lde;
    }
}
