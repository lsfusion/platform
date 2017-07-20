package lsfusion.server.context;

public abstract class SameThreadExecutionStack extends UpExecutionStack {

    public SameThreadExecutionStack(ExecutionStack upStack) {
        super(upStack);
    }
}
