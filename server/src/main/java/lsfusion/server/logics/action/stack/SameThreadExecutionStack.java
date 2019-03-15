package lsfusion.server.logics.action.stack;

public abstract class SameThreadExecutionStack extends UpExecutionStack {

    public SameThreadExecutionStack(ExecutionStack upStack) {
        super(upStack);
    }
}
