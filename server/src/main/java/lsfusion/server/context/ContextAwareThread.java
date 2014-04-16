package lsfusion.server.context;

public class ContextAwareThread extends Thread {
    private final Context context;

    public ContextAwareThread(Context context, Runnable target) {
        super(target);
        this.context = context;
    }
    public ContextAwareThread(Context context, ThreadGroup group, Runnable target, String name, long stackSize) {
        super(group, target, name, stackSize);
        this.context = context;
    }

    @Override
    public void run() {
        ThreadLocalContext.set(context);
        super.run();
        context.getLogicsInstance().getDbManager().closeThreadLocalSql();
    }
}
