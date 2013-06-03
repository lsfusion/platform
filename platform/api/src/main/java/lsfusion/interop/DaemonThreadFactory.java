package lsfusion.interop;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class DaemonThreadFactory implements ThreadFactory {
    private static final AtomicInteger poolNumber = new AtomicInteger(1);
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;

    public DaemonThreadFactory(String threadNamePrefix) {
        SecurityManager s = System.getSecurityManager();
        group = s != null
                ? s.getThreadGroup()
                : Thread.currentThread().getThreadGroup();
        this.namePrefix = "pool-" + poolNumber.getAndIncrement() + threadNamePrefix;
    }

    public Thread newThread(Runnable r) {
        Thread t = newThreadInstance(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
        if (!t.isDaemon()) {
            t.setDaemon(true);
        }
        if (t.getPriority() != Thread.NORM_PRIORITY) {
            t.setPriority(Thread.NORM_PRIORITY);
        }
        return t;
    }

    protected Thread newThreadInstance(ThreadGroup group, Runnable r, String name, int stackSize) {
        return new Thread(group, r, name, stackSize);
    }
}
