package lsfusion.server.context;

public interface NewThreadExecutionStack extends ExecutionStack {

    boolean checkStack(NewThreadExecutionStack stack);
}
