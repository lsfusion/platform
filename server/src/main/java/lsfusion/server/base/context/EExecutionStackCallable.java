package lsfusion.server.base.context;

public interface EExecutionStackCallable<R> {

    R call(ExecutionStack stack) throws Exception;
}
