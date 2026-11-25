package lsfusion.server.physics.admin.log;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// #2025: a start step stuck on one property has to name it in the start log
public class StartStepWatchdogTest {

    private final List<String> warnings = new CopyOnWriteArrayList<>();
    private final AppenderSkeleton appender = new AppenderSkeleton() {
        protected void append(LoggingEvent event) {
            if (event.getLevel() == Level.WARN)
                warnings.add(event.getRenderedMessage());
        }

        public void close() {
        }

        public boolean requiresLayout() {
            return false;
        }
    };

    @Before
    public void addAppender() {
        ServerLoggers.startLogger.addAppender(appender);
    }

    @After
    public void removeAppender() {
        ServerLoggers.startLogger.removeAppender(appender);
    }

    @Test
    public void aSlowItemIsNamedWhileRunningAndWhenDone() throws InterruptedException {
        try (StartStepWatchdog watchdog = new StartStepWatchdog("Step", 1)) {
            watchdog.start("the value of A", "a hint");
            // the item stays running until the watchdog has named it, however late its check comes
            for (int i = 0; i < 100 && warnings.isEmpty(); i++)
                Thread.sleep(100);
            watchdog.finish();
        }
        assertEquals(warnings.toString(), 2, warnings.size());
        assertEquals("Step: computing the value of A is still running after 1 s (a hint)", warnings.get(0));
        assertTrue(warnings.get(1), warnings.get(1).matches("Step: computing the value of A took [1-9][0-9]* s \\(a hint\\)"));
    }

    @Test
    public void aFastItemIsNotNamed() {
        try (StartStepWatchdog watchdog = new StartStepWatchdog("Step", 1)) {
            watchdog.start("the value of A", "a hint");
            watchdog.finish();
        }
        assertTrue(warnings.toString(), warnings.isEmpty());
    }

    @Test
    public void aZeroLimitTurnsTheWarningsOff() throws InterruptedException {
        try (StartStepWatchdog watchdog = new StartStepWatchdog("Step", 0)) {
            watchdog.start("the value of A", "a hint");
            Thread.sleep(1500);
            watchdog.finish();
        }
        assertTrue(warnings.toString(), warnings.isEmpty());
    }
}
