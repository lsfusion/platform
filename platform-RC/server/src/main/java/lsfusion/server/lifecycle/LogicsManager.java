package lsfusion.server.lifecycle;

import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.context.ExecutionStack;
import lsfusion.server.logics.LogicsInstance;

// составные части LogicsInstance
public abstract class LogicsManager extends LifecycleAdapter {

    protected ExecutionStack getStack() {
        ThreadLocalContext.assureLifecycle(ThreadLocalContext.getLogicsInstance()); // в остальных местах ExecutionStack должен быть параметром
        return ThreadLocalContext.getStack();
    }

    public LogicsManager() {
    }

    public LogicsManager(int order) {
        super(order);
    }
}
