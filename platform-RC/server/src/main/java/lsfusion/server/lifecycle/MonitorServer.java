package lsfusion.server.lifecycle;

import lsfusion.server.context.ExecutionStack;
import lsfusion.server.context.ThreadLocalContext;

public abstract class MonitorServer extends EventServer implements MonitorServerInterface {

    public MonitorServer() {
    }

    public MonitorServer(int order) {
        super(order);
    }

    protected ExecutionStack getStack() {
        ThreadLocalContext.assureMonitor(this);
        return ThreadLocalContext.getStack();
    }
}
