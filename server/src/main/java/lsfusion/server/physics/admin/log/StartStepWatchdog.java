package lsfusion.server.physics.admin.log;

import lsfusion.base.DaemonThreadFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static lsfusion.server.physics.admin.log.ServerLoggers.startLogWarn;

// Names the item a start step is stuck on: a start step that iterates over all properties marks the item it is computing,
// and once one item takes longer than the limit, a WARN names it (while it still runs, and again with the total time when it is done)
public class StartStepWatchdog implements AutoCloseable {

    private static class Item {
        private final String name;
        private final String hint;
        private final long start = System.nanoTime();
        private volatile boolean warned;

        private Item(String name, String hint) {
            this.name = name;
            this.hint = hint;
        }

        private long getMillis() {
            return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        }
    }

    private final String step;
    private final long warnMillis;
    private final ScheduledExecutorService scheduler;

    private volatile Item current;

    // warnSeconds <= 0 turns the watchdog off
    public StartStepWatchdog(String step, int warnSeconds) {
        this.step = step;
        this.warnMillis = TimeUnit.SECONDS.toMillis(warnSeconds);
        if (warnSeconds > 0) {
            scheduler = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory("start-step-watchdog"));
            scheduler.scheduleWithFixedDelay(this::check, 1, 1, TimeUnit.SECONDS);
        } else
            scheduler = null;
    }

    // name - what is being computed, hint - how to avoid computing it
    public void start(String name, String hint) {
        if (scheduler != null)
            current = new Item(name, hint);
    }

    public void finish() {
        Item item = current;
        if (item != null) {
            current = null;
            long millis = item.getMillis();
            if (millis >= warnMillis)
                startLogWarn(getMessage(item, "took " + formatSeconds(millis)));
        }
    }

    private void check() {
        Item item = current;
        if (item != null && !item.warned && item.getMillis() >= warnMillis) {
            item.warned = true;
            startLogWarn(getMessage(item, "is still running after " + formatSeconds(warnMillis)));
        }
    }

    private String getMessage(Item item, String state) {
        return step + ": computing " + item.name + " " + state + " (" + item.hint + ")";
    }

    private static String formatSeconds(long millis) {
        return (millis / 1000) + " s";
    }

    @Override
    public void close() {
        if (scheduler != null)
            scheduler.shutdownNow();
    }
}
