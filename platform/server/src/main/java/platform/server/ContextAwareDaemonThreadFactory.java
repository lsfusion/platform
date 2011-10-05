package platform.server;

import platform.interop.DaemonThreadFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class ContextAwareDaemonThreadFactory extends DaemonThreadFactory {
    private static final AtomicInteger poolNumber = new AtomicInteger(1);

    private final Context context;
    private final String namePrefix;

    public ContextAwareDaemonThreadFactory(Context context) {
        this.context = context;
        namePrefix = "pool-" + poolNumber.getAndIncrement() + "-context-aware-thread-";
    }

    protected Thread newThreadInstance(Runnable r) {
        return new ContextAwareThread(context, group, r, namePrefix + threadNumber.getAndIncrement(), 0);
    }

}
