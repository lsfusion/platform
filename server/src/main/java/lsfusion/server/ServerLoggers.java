package lsfusion.server;

import lsfusion.base.ExceptionUtils;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.interop.DaemonThreadFactory;
import lsfusion.logging.FlushableRollingFileAppender;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.form.navigator.SQLSessionUserProvider;
import org.apache.log4j.Logger;

import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    public static final Logger lruLogger = Logger.getLogger("LRULogger");

    public static final Logger assertLogger = Logger.getLogger("AssertLogger");

    public static final Logger exInfoLogger = Logger.getLogger("ExInfoLogger");

    public static final Logger jdbcLogger = Logger.getLogger("JDBCLogger");

    public static final Logger scriptLogger = Logger.getLogger("ScriptLogger");

    public static final Logger pausablesInvocationLogger = Logger.getLogger("PausableInvocationsLogger");

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
            assertLogger.info(message + '\n' + ExceptionUtils.getStackTrace());
        assert assertion : message;
    }

    public static void sqlSuppLog(Throwable t) {
        sqlHandLogger.error("SUPPRESSED : " + t.toString() + '\n' + ExceptionUtils.getStackTrace(t));
    }
    
    public static void handledLog(String message) {
        sqlHandLogger.info(message + '\n' + ExceptionUtils.getStackTrace());
    }

    public static void handledLog(ImList<String> messages) {
        String result = "";
        String tab = "";
        for(String message : messages) {
            result += '\n' + tab + message;
            tab += '\t';
        }
        handledLog(result);
    }

    public static void exinfoLog(String message) {
        exInfoLogger.info(message + '\n' + ExceptionUtils.getStackTrace());
    }

    private static Map<Integer, Boolean> userExLogs = new ConcurrentHashMap<>();

    public static void setUserExLog(Integer user, Boolean enabled) {
        userExLogs.put(user, enabled != null && enabled);
    }

    public static boolean getUserExLog(Integer user) {
        Boolean useLog = userExLogs.get(user);
        return useLog != null && useLog;
    }

    public static boolean isUserExLog() {
        return getUserExLog(ThreadLocalContext.get().getCurrentUser());
    }

}
