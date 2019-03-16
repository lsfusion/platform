package lsfusion.server.base.controller.manager;

import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;

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
