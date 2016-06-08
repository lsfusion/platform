package lsfusion.server.context;

import lsfusion.server.session.DataSession;

public class SyncExecutionStack extends UpExecutionStack implements NewThreadExecutionStack {

    private final String threadId;

    public SyncExecutionStack(String threadId, ExecutionStack upStack) {
        super(upStack);
        this.threadId = threadId;
    }

    @Override
    protected DataSession getSession() {
        return null;
    }

    public static NewThreadExecutionStack newThread(ExecutionStack upStack, String threadId, SyncType type) {
        if(type == SyncType.SYNC)
            return new SyncExecutionStack(threadId, upStack);
        return new TopExecutionStack(threadId);
    }

    @Override
    public boolean checkStack(NewThreadExecutionStack stack) {
        if(!(stack instanceof SyncExecutionStack))
            return false;
        return threadId.equals(((SyncExecutionStack)stack).threadId) && upStack.equals(((SyncExecutionStack)stack).upStack);
    }
}
