package lsfusion.server.logics.action.flow;

import lsfusion.server.base.controller.stack.ExecutionStackAspect;

public class RuntimeExceptionWithStack extends RuntimeException {
    public String lsfStack;

    public RuntimeExceptionWithStack(Throwable t) {
        super(t);
        this.lsfStack = ExecutionStackAspect.getExceptionStackTrace();
    }
}
