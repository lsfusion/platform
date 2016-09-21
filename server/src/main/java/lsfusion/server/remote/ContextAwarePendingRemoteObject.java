package lsfusion.server.remote;

import lsfusion.base.BaseUtils;
import lsfusion.base.ExceptionUtils;
import lsfusion.base.WeakIdentityHashSet;
import lsfusion.interop.remote.PendingRemoteObject;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.context.*;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.ThreadUtils;
import org.apache.log4j.Logger;

import java.rmi.RemoteException;
import java.rmi.server.Unreferenced;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;

public abstract class ContextAwarePendingRemoteObject extends PendingRemoteObject implements Unreferenced {

    protected final static Logger logger = ServerLoggers.systemLogger;

    private final WeakIdentityHashSet<Thread> threads = new WeakIdentityHashSet<>();

    protected ExecutionStack getStack() {
        ThreadLocalContext.assureRmi(this);
        return ThreadLocalContext.getStack();
    }

    private NewThreadExecutionStack rmiStack;
    // по сути private (не использовать в явную, вместо него использовать getStack), так как должен быть private в ThreadLocalContext, но в ООП так нельзя
    public NewThreadExecutionStack getRmiStack() {
        return rmiStack;
    }

    protected Context context;

    protected ExecutorService pausablesExecutor;

    protected ContextAwarePendingRemoteObject() {
        super();
        pausablesExecutor = null;
        rmiStack = new TopExecutionStack(getSID());
    }

    protected void finalizeInit(ExecutionStack upStack, SyncType type) {
        pausablesExecutor = ExecutorFactory.createRmiMirrorSyncService(this);
        rmiStack = SyncExecutionStack.newThread(upStack, getSID(), type);
    }

    protected ContextAwarePendingRemoteObject(int port) throws RemoteException {
        super(port);
    }

    // not used
    protected ContextAwarePendingRemoteObject(int port, boolean autoExport) throws RemoteException {
        super(port, autoExport);
        assert false;
        pausablesExecutor = null;
        rmiStack = new TopExecutionStack(getSID());
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public String getRemoteActionMessage() throws RemoteException {
        return ThreadLocalContext.getActionMessage();
    }

    public List<Object> getRemoteActionMessageList() throws RemoteException {
        return ThreadLocalContext.getActionMessageList();
    }

    public void addLinkedThread(Thread thread) {
        synchronized (threads) {
            threads.add(thread);
        }
    }

    public void removeLinkedThread(Thread thread) {
        synchronized (threads) {
            threads.remove(thread);
        }
    }

    public abstract String getSID();

    protected boolean isUnreferencedSyncedClient() {
        return false;
    }

    @Override
    public void unreferenced() {
        ThreadLocalContext.aspectBeforeRmi(this, true);
        try {
            deactivateAndCloseLater(isUnreferencedSyncedClient());
        } finally {
            ThreadLocalContext.aspectAfterRmi();
        }
    }

    // явная очистка ресурсов, которые поддерживаются через weak ref'ы
    protected void onClose() {
        if (pausablesExecutor != null)
            pausablesExecutor.shutdown();
    }

    public boolean isDeactivated() {
        return deactivated;
    }

    private boolean deactivated = false;
    // умертвляет объект - отключает его от новых потоков + закрывает все старые потоки
    // ВАЖНО что должно выполняться в потоке, который сам не попадает в cleanThreads
    public synchronized void deactivate() {
        if(deactivated)
            return;

        ServerLoggers.remoteLifeLog("REMOTE OBJECT DEACTIVATE " + this);

        onDeactivate();

        deactivated = true;
    }

    protected void onDeactivate() {
        unexport();

        synchronized (threads) {
            for (Thread thread : threads) {
                ServerLoggers.exinfoLog("FORCEFULLY STOPPED : " + thread + '\n' + ExceptionUtils.getStackTrace() + '\n' + ExceptionUtils.getStackTrace(thread.getStackTrace()));
                try {
                    ThreadUtils.interruptThread(context, thread);
                } catch (SQLException | SQLHandledException ignored) {
                    ServerLoggers.sqlSuppLog(ignored);
                } catch (Throwable t) {
                    ServerLoggers.sqlSuppLog(t); // пока сюда же выведем
                }
            }
        }
    }

    public synchronized void deactivateAndCloseLater(final boolean syncedOnClient) {
        if(Settings.get().isDisableAsyncClose() && !syncedOnClient)
            return;

        final int delay = Settings.get().getCloseFormDelay();
        BaseUtils.runLater(delay, new Runnable() { // тут надо бы на ContextAwareDaemonThreadFactory переделать
            public void run() {
                ThreadLocalContext.aspectBeforeRmi(ContextAwarePendingRemoteObject.this, true);
                try {
                    deactivate();

                    try {
                        Thread.sleep(delay); // даем время на deactivate (interrupt)
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    explicitClose();
                } finally {
                    ThreadLocalContext.aspectAfterRmi();
                }
            }
        });
    }

    private boolean closed;
    public synchronized void explicitClose() {
        ServerLoggers.assertLog(deactivated, "REMOTE OBJECT MUST BE DEACTIVATED " + this);
        if(closed)
            return;

        ServerLoggers.remoteLifeLog("REMOTE OBJECT CLOSE " + this);

        onClose();

        closed = true;
    }

    public boolean isClosed() {
        return closed;
    }

    public void logServerException(Throwable t) throws SQLException, SQLHandledException {
        BusinessLogics businessLogics = getContext().getLogicsInstance().getBusinessLogics();
        businessLogics.systemEventsLM.logException(businessLogics, getStack(), t, null, null, false, false);
    }
}
