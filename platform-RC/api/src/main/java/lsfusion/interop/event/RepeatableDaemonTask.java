package lsfusion.interop.event;

import com.google.common.base.Throwables;
import lsfusion.interop.DaemonThreadFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class RepeatableDaemonTask extends AbstractDaemonTask {
    protected final ScheduledExecutorService executor;

    protected int delay;
    protected int period;
    protected String threadPoolName;

    public RepeatableDaemonTask(int delay, int period, String threadPoolName) {
        this.delay = delay;
        this.period = period;
        this.threadPoolName = threadPoolName;
        executor = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory(threadPoolName));
    }

    @Override
    public final void start() {
        executor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    tick();
                } catch (Exception e) {
                    //TODO: shutdown executor?
                    logger.error("Error running daemon task: " + threadPoolName, e);
                    throw Throwables.propagate(e);
                }
            }
        }, delay, period, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        executor.shutdown();
        try {
            executor.awaitTermination(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw Throwables.propagate(e);
        }
    }

    protected abstract void tick();

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }
}
