package lsfusion.server.logics.action.stack;

public interface EExecutionStackCallable<R> {

    R call(ExecutionStack stack) throws Exception;
}
