package lsfusion.server.base.context;

public abstract class SameThreadExecutionStack extends UpExecutionStack {

    public SameThreadExecutionStack(ExecutionStack upStack) {
        super(upStack);
    }
}
