package lsfusion.server.physics.admin.log;

import lsfusion.base.DaemonThreadFactory;
import lsfusion.base.ExceptionUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.lambda.E2Runnable;
import lsfusion.base.log.FlushableRollingFileAppender;
import lsfusion.server.base.controller.stack.ExecutionStackAspect;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.SystemProperties;
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

    public static final Logger processDumpLogger = Logger.getLogger("ProcessDumpLogger");

    public static final Logger sqlHandLogger = Logger.getLogger("SQLHandLogger");

    public static final Logger sqlAdjustLogger = Logger.getLogger("SQLAdjustLogger");

    public static final Logger sqlConflictLogger = Logger.getLogger("SQLConflictLogger");

    public static final Logger sqlConnectionLogger = Logger.getLogger("SQLConnectionLogger");

    public static final Logger lruLogger = Logger.getLogger("LRULogger");

    public static final Logger allocatedBytesLogger = Logger.getLogger("AllocatedBytesLogger");

    public static final Logger importLogger = Logger.getLogger("ImportLogger");

    public static final Logger printerLogger = Logger.getLogger("PrinterLogger");

    public static final Logger assertLogger = Logger.getLogger("AssertLogger");

    public static final Logger exInfoLogger = Logger.getLogger("ExInfoLogger");

    public static final Logger hExInfoLogger = Logger.getLogger("HExInfoLogger");

    public static final Logger jdbcLogger = Logger.getLogger("JDBCLogger");

    public static final Logger pausablesInvocationLogger = Logger.getLogger("PausableInvocationsLogger");
    
    public static final Logger explainLogger = Logger.getLogger("ExplainLogger");

    public static final Logger explainAppLogger = Logger.getLogger("ExplainAppLogger");

    public static final Logger explainCompileLogger = Logger.getLogger("ExplainCompileLogger");
    
    public static final Logger startLogger = Logger.getLogger("StartLogger");

    public static final Logger schedulerLogger = Logger.getLogger("SchedulerLogger");

    public static final Logger httpServerLogger = Logger.getLogger("HttpServerLogger");

    public static final Logger httpFromExternalSystemRequestsLogger = Logger.getLogger("HttpFromExternalSystemRequestsLogger");

    public static final Logger httpToExternalSystemRequestsLogger = Logger.getLogger("HttpToExternalSystemRequestsLogger");

    private static final int FORCE_FLUSH_DELAY = 60;

    static {
        Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory("log4j-flusher"))
                .scheduleWithFixedDelay(() -> {
                    Enumeration appenders = pausablesInvocationLogger.getAllAppenders();
                    while (appenders.hasMoreElements()) {
                        Object nextAppender = appenders.nextElement();
                        if (nextAppender instanceof FlushableRollingFileAppender) {
                            ((FlushableRollingFileAppender) nextAppender).flush();
                        }
                    }
                }, FORCE_FLUSH_DELAY, FORCE_FLUSH_DELAY, TimeUnit.SECONDS);
    }

    public static void assertLog(boolean assertion, String message) {
        assertLog(assertion, message, false);
    }
    public static void assertLog(boolean assertion, String message, boolean interactive) {
        if(!assertion) {
            Settings settings;
            if(interactive && (settings = Settings.get()) != null && settings.isEnableInteractiveAssertLog())
                ThreadLocalContext.message(ThreadLocalContext.localize("{logics.server.interactive.assert}"));
            assertLogger.info(message + '\n' + ExecutionStackAspect.getExStackTrace());
        }
        assert assertion : message;
    }

    public static void sqlSuppLog(Throwable t) {
        sqlHandLogger.error("SUPPRESSED : " + ExceptionUtils.toString(t) + '\n' + ExecutionStackAspect.getExStackTrace());
    }
    
    public static void handledExLog(String message) {
        sqlHandLogger.info(message + '\n' + ExecutionStackAspect.getExStackTrace());
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
        exInfoLogger.info(message);
        if (exInfoLogger.isTraceEnabled())
            exInfoLogger.trace(ExceptionUtils.getStackTrace());
    }

    public static void remoteLifeLog(String message) {
        remoteLogger.info(message);
        if (remoteLogger.isTraceEnabled())
            remoteLogger.trace(ExceptionUtils.getStackTrace());
    }

    public static void sqlConnectionLog(String message) {
        sqlConnectionLogger.info(message);
        if (sqlConnectionLogger.isTraceEnabled())
            sqlConnectionLogger.trace(ExceptionUtils.getStackTrace());
    }

    private static Map<Long, Boolean> userExLogs = MapFact.getGlobalConcurrentHashMap();

    public static void setUserExLog(Long user, Boolean enabled) {
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

    public static boolean getUserExLog(Long user) {
        Boolean useLog = userExLogs.get(user);
        return useLog != null && useLog;
    }

    public static boolean isUserExLog() {
        return enabledUserExLog > 0 && getUserExLog(ThreadLocalContext.getCurrentUser());
    }

    private static Map<Long, Boolean> pausableLogs = MapFact.getGlobalConcurrentHashMap();

    public static void setPausableLog(Long user, Boolean enabled) {
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

    public static boolean isPausableLogEnabled(Long user) {
        Boolean useLog = pausableLogs.get(user);
        return useLog != null && useLog;
    }
    public static boolean isPausableLogEnabled() {
        return SystemProperties.inTestMode || (enabledPausableLog > 0 && isPausableLogEnabled(ThreadLocalContext.get().getCurrentUser()));
    }
    public static void pausableLog(String s) {
        if(isPausableLogEnabled())
            pausablesInvocationLogger.info(s);
    }

    public static void pausableLog(String s, Throwable t) {
        if(isPausableLogEnabled())
            pausablesInvocationLogger.debug(s);
    }

    public static void startLogDebug(String message) {
        startLogger.debug(message);
    }

    public static void startLogWarn(String message) {
        startLogger.warn(message);
    }

    public static void startLogError(String message) {
        startLogger.error(message);
    }

    public static void startLogError(String message, Throwable t) {
        startLogger.error(message, t);
    }

    public static void startLog(String message) {
        startLogger.info(message);
    }

    public static <E1 extends Exception, E2 extends Exception> void runWithStartLog(E2Runnable<E1, E2> run, String message) throws E1, E2 {
        runWithLog(run, message, startLogger);
    }

    public static <E1 extends Exception, E2 extends Exception> void runWithServiceLog(E2Runnable<E1, E2> run, String message) throws E1, E2 {
        runWithLog(run, message, serviceLogger);
    }

    public static <E1 extends Exception, E2 extends Exception> void runWithLog(E2Runnable<E1, E2> run, String message, Logger logger) throws E1, E2 {
        long start = System.currentTimeMillis();
        logger.info(message + " started");
        run.run();
        logger.info(message + " finished, " + (System.currentTimeMillis() - start) + "ms");
    }
}
