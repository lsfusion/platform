package lsfusion.server.remote;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.form.navigator.RemoteNavigator;
import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Aspect
public class RemoteLoggerAspect {
    private final static Logger logger = ServerLoggers.remoteLogger;

    public static final Map<Integer, Long> userActivityMap = MapFact.getGlobalConcurrentHashMap();
    public static final Map<Integer, Map<Long, List<Long>>> pingInfoMap = MapFact.getGlobalConcurrentHashMap();
    private static final Map<Long, Timestamp> dateTimeCallMap = MapFact.getGlobalConcurrentHashMap();
    private static Map<Integer, Boolean> remoteLoggerDebugEnabled = MapFact.getGlobalConcurrentHashMap();

    @Around("(execution(* (lsfusion.interop.RemoteLogicsInterface+ && *..*Interface).*(..))" +
            " || execution(* lsfusion.interop.form.RemoteFormInterface.*(..))" +
            " || execution(* lsfusion.interop.navigator.RemoteNavigatorInterface.*(..)))" +
            " && !execution(* *.ping(..))" +
            "&& target(target)")
    public Object executeRemoteMethod(ProceedingJoinPoint thisJoinPoint, Object target) throws Throwable {
        final long id = Thread.currentThread().getId();
        putDateTimeCall(id, new Timestamp(System.currentTimeMillis()));
        try {
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

            userActivityMap.put(user, startTime);

            boolean debugEnabled = user != null && isRemoteLoggerDebugEnabled(user);

            if (debugEnabled || runTime > Settings.get().getRemoteLogTime()) {
                logger.info(logCall(thisJoinPoint, runTime));
            }

            return result;
        } finally {
            removeDateTimeCall(id);
        }
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

    public static Timestamp getDateTimeCall(long pid) {
        return dateTimeCallMap.get(pid);
    }

    public static void putDateTimeCall(long pid, Timestamp timestamp) {
        dateTimeCallMap.put(pid, timestamp);
    }

    public static void removeDateTimeCall(long pid) {
        dateTimeCallMap.remove(pid);
    }
}
