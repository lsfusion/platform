package lsfusion.server;

import lsfusion.base.ExceptionUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.interop.DaemonThreadFactory;
import lsfusion.logging.FlushableRollingFileAppender;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.stack.ExecutionStackAspect;
import org.apache.log4j.Logger;

import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ServerLoggers {
    public static final Logger systemLogger = Logger.getLogger("SystemLogger");

    public static final Logger serviceLogger = Logger.getLogger("ServiceLogger");

    public static final Logger remoteLogger = Logger.getLogger("RemoteLogger");

    public static final Logger mailLogger = Logger.getLogger("MailLogger");

    public static final Logger sqlLogger = Logger.getLogger("SQLLogger");

    public static final Logger securityLogger = Logger.getLogger("SecurityLogger");

    public static final Logger sqlHandLogger = Logger.getLogger("SQLHandLogger");

    public static final Logger sqlAdjustLogger = Logger.getLogger("SQLAdjustLogger");

    public static final Logger sqlConflictLogger = Logger.getLogger("SQLConflictLogger");

    public static final Logger lruLogger = Logger.getLogger("LRULogger");

    public static final Logger allocatedBytesLogger = Logger.getLogger("AllocatedBytesLogger");

    public static final Logger importLogger = Logger.getLogger("ImportLogger");

    public static final Logger printerLogger = Logger.getLogger("PrinterLogger");

    public static final Logger assertLogger = Logger.getLogger("AssertLogger");

    public static final Logger exInfoLogger = Logger.getLogger("ExInfoLogger");

    public static final Logger hExInfoLogger = Logger.getLogger("HExInfoLogger");

    public static final Logger jdbcLogger = Logger.getLogger("JDBCLogger");

    public static final Logger scriptLogger = Logger.getLogger("ScriptLogger");

    public static final Logger pausablesInvocationLogger = Logger.getLogger("PausableInvocationsLogger");
    
    public static final Logger explainLogger = Logger.getLogger("ExplainLogger");
    
    public static final Logger startLogger = Logger.getLogger("StartLogger");

    public static final Logger schedulerLogger = Logger.getLogger("SchedulerLogger");

    private static final int FORCE_FLUSH_DELAY = 60;

    static {
        Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory("log4j-flusher"))
                .scheduleWithFixedDelay(new Runnable() {
                    @Override
                    public void run() {
                        Enumeration appenders = pausablesInvocationLogger.getAllAppenders();
                        while (appenders.hasMoreElements()) {
                            Object nextAppender = appenders.nextElement();
                            if (nextAppender instanceof FlushableRollingFileAppender) {
                                ((FlushableRollingFileAppender) nextAppender).flush();
                            }
                        }
                    }
                }, FORCE_FLUSH_DELAY, FORCE_FLUSH_DELAY, TimeUnit.SECONDS);
    }
    
    public static void assertLog(boolean assertion, String message) {
        if(!assertion)
            assertLogger.info(message + '\n' + ExecutionStackAspect.getExStackTrace());
        assert assertion : message;
    }

    public static void sqlSuppLog(Throwable t) {
        sqlHandLogger.error("SUPPRESSED : " + t.toString() + '\n' + ExceptionUtils.getStackTrace(t));
    }
    
    public static void handledLog(String message) {
        sqlHandLogger.info(message + '\n' + ExceptionUtils.getStackTrace());
    }

    public static void adjustLog(String message) {
        sqlAdjustLogger.info(message + '\n' + ExceptionUtils.getStackTrace());
    }

    public static void adjustLog(ImList<String> messages, boolean lsfStack) {
        String result = "";
        String tab = "";
        for(String message : messages) {
            result += '\n' + tab + message;
            tab += '\t';
        }
        if(lsfStack)
            result += ExecutionStackAspect.getStackString() + '\n';
        adjustLog(result);
    }

    public static void exinfoLog(String message) {
        exInfoLogger.info(message + '\n' + ExceptionUtils.getStackTrace());
    }

    public static void remoteLifeLog(String message) {
        remoteLogger.info(message + '\n' + ExceptionUtils.getStackTrace());
    }

    private static Map<Integer, Boolean> userExLogs = MapFact.getGlobalConcurrentHashMap();

    public static void setUserExLog(Integer user, Boolean enabled) {
        final boolean newEnabled = enabled != null && enabled;
        final Boolean prevBEnabled = userExLogs.put(user, newEnabled);
        final boolean prevEnabled = prevBEnabled != null && prevBEnabled;
        if(newEnabled != prevEnabled) {
            if (newEnabled)
                enabledUserExLog++;
            else
                enabledUserExLog--;
        }
    }

    public static int enabledUserExLog;

    public static boolean getUserExLog(Integer user) {
        Boolean useLog = userExLogs.get(user);
        return useLog != null && useLog;
    }

    public static boolean isUserExLog() {
        return enabledUserExLog > 0 && getUserExLog(ThreadLocalContext.getCurrentUser());
    }

    private static Map<Integer, Boolean> pausableLogs = MapFact.getGlobalConcurrentHashMap();

    public static void setPausableLog(Integer user, Boolean enabled) {
        final boolean newEnabled = enabled != null && enabled;
        final Boolean prevBEnabled = pausableLogs.put(user, newEnabled);
        final boolean prevEnabled = prevBEnabled != null && prevBEnabled;
        if(newEnabled != prevEnabled) {
            if (newEnabled)
                enabledPausableLog++;
            else
                enabledPausableLog--;
        }
    }

    public static int enabledPausableLog;

    public static boolean isPausableLogEnabled(Integer user) {
        Boolean useLog = pausableLogs.get(user);
        return useLog != null && useLog;
    }
    public static boolean isPausableLogEnabled() {
        return SystemProperties.isDebug || (enabledPausableLog > 0 && isPausableLogEnabled(ThreadLocalContext.get().getCurrentUser()));
    }
    public static void pausableLog(String s) {
        if(isPausableLogEnabled())
            pausablesInvocationLogger.info(s);
    }

    public static void pausableLogStack(String s) {
        if(isPausableLogEnabled())
            pausablesInvocationLogger.info(s + '\n' + ExceptionUtils.getStackTrace());
    }

    public static void pausableLog(String s, Throwable t) {
        if(isPausableLogEnabled())
            pausablesInvocationLogger.debug(s);
    }
}
