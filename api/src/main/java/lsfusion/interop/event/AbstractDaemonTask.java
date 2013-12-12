package lsfusion.interop.event;

import org.apache.log4j.Logger;

public abstract class AbstractDaemonTask implements IDaemonTask {
    protected static final Logger logger = Logger.getLogger(AbstractDaemonTask.class);

    protected EventBus eventBus;

    @Override
    public abstract void start() throws Exception;

    @Override
    public abstract void stop();

    @Override
    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }
}
