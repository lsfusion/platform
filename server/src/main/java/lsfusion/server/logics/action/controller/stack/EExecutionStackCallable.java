package lsfusion.server.logics.action.controller.stack;

public interface EExecutionStackCallable<R> {

    R call(ExecutionStack stack) throws Exception;
}
