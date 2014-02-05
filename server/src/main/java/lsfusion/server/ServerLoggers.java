package lsfusion.server;

import lsfusion.base.ExceptionUtils;
import lsfusion.interop.DaemonThreadFactory;
import lsfusion.logging.FlushableRollingFileAppender;
import org.apache.log4j.Logger;

import java.util.Enumeration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ServerLoggers {
    public static final Logger systemLogger = Logger.getLogger("SystemLogger");

    public static final Logger remoteLogger = Logger.getLogger("RemoteLogger");

    public static final Logger mailLogger = Logger.getLogger("MailLogger");

    public static final Logger sqlLogger = Logger.getLogger("SQLLogger");

    public static final Logger sqlHandLogger = Logger.getLogger("SQLHandLogger");

    public static final Logger lruLogger = Logger.getLogger("LRULogger");

    public static final Logger assertLogger = Logger.getLogger("AssertLogger");

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
        assert assertion;
    }
}
