package lsfusion.client;

import lsfusion.interop.DaemonThreadFactory;
import lsfusion.logging.FlushableRollingFileAppender;
import org.apache.log4j.Logger;

import java.util.Enumeration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ClientLoggers {
    public static final Logger systemLogger = Logger.getLogger("SystemLogger");
    
    public static final Logger clientLogger = Logger.getLogger("ClientLogger");

    public static final Logger remoteLogger = Logger.getLogger("RemoteLogger");

    public static final Logger invocationLogger = Logger.getLogger("InvocationsLogger");

    private static final int FORCE_FLUSH_DELAY = 30;

    static {
        Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory("log4j-flusher"))
                .scheduleWithFixedDelay(new Runnable() {
                    @Override
                    public void run() {
                        Enumeration appenders = invocationLogger.getAllAppenders();
                        while (appenders.hasMoreElements()) {
                            Object nextAppender = appenders.nextElement();
                            if (nextAppender instanceof FlushableRollingFileAppender) {
                                ((FlushableRollingFileAppender) nextAppender).flush();
                            }
                        }
                    }
                }, FORCE_FLUSH_DELAY, FORCE_FLUSH_DELAY, TimeUnit.SECONDS);
    }
}
