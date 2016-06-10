package lsfusion.server.lifecycle;

import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.context.ExecutionStack;
import lsfusion.server.logics.LogicsInstance;

public abstract class EventServer extends LifecycleAdapter {

    public EventServer() {
    }

    public EventServer(int order) {
        super(order);
    }

    public abstract String getEventName();

    public abstract LogicsInstance getLogicsInstance();

}
