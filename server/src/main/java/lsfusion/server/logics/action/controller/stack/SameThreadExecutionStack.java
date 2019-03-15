package lsfusion.server.logics.action.controller.stack;

public abstract class SameThreadExecutionStack extends UpExecutionStack {

    public SameThreadExecutionStack(ExecutionStack upStack) {
        super(upStack);
    }
}
