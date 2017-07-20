package lsfusion.server.context;

public interface EExecutionStackCallable<R> {

    R call(ExecutionStack stack) throws Exception;
}
