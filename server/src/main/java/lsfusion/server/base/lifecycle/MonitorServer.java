package lsfusion.server.base.lifecycle;

import lsfusion.server.logics.action.stack.ExecutionStack;
import lsfusion.server.base.thread.ThreadLocalContext;

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
