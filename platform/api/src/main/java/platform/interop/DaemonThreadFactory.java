package platform.interop;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class DaemonThreadFactory implements ThreadFactory {
    private static final AtomicInteger poolNumber = new AtomicInteger(1);
    protected final ThreadGroup group;
    protected final AtomicInteger threadNumber = new AtomicInteger(1);
    protected final String namePrefix;

    public DaemonThreadFactory() {
        SecurityManager s = System.getSecurityManager();
        group = s != null
                ? s.getThreadGroup()
                : Thread.currentThread().getThreadGroup();
        namePrefix = "pool-" + poolNumber.getAndIncrement() + "-thread-";
    }

    public Thread newThread(Runnable r) {
        Thread t = newThreadInstance(r);
        if (!t.isDaemon()) {
            t.setDaemon(true);
        }
        if (t.getPriority() != Thread.NORM_PRIORITY) {
            t.setPriority(Thread.NORM_PRIORITY);
        }
        return t;
    }

    protected Thread newThreadInstance(Runnable r) {
        return new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
    }
}
