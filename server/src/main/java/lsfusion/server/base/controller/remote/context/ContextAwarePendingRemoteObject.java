package lsfusion.server.base.controller.remote.context;

import lsfusion.base.BaseUtils;
import lsfusion.base.ExceptionUtils;
import lsfusion.base.col.heavy.weak.WeakIdentityHashSet;
import lsfusion.server.base.controller.context.Context;
import lsfusion.server.base.controller.remote.PendingRemoteObject;
import lsfusion.server.base.controller.stack.ExecutionStackAspect;
import lsfusion.server.base.controller.thread.*;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.controller.stack.NewThreadExecutionStack;
import lsfusion.server.logics.action.controller.stack.SyncExecutionStack;
import lsfusion.server.logics.action.controller.stack.TopExecutionStack;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.admin.profiler.ProfiledObject;
import org.apache.log4j.Logger;

import java.rmi.RemoteException;
import java.rmi.server.Unreferenced;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class ContextAwarePendingRemoteObject extends PendingRemoteObject implements Unreferenced, ProfiledObject { // AutoCloseable in offline mode (with port = -1)

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

    private static ScheduledExecutorService closeExecutor = ExecutorFactory.createCloseScheduledThreadService(3);
    private void scheduleClose(long delay, Runnable run) {
        closeExecutor.schedule(() -> {
            ThreadInfo threadInfo = EventThreadInfo.TIMER(ContextAwarePendingRemoteObject.this);
            ThreadLocalContext.aspectBeforeRmi(ContextAwarePendingRemoteObject.this, true, threadInfo);
            try {
                run.run();
            } catch (Throwable t) {
                ServerLoggers.remoteLogger.error("FORM CLOSE: ", t);
            } finally {
                ThreadLocalContext.aspectAfterRmi(threadInfo);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    protected ExecutorService pausablesExecutor;
    private final String sID;

    protected ContextAwarePendingRemoteObject(String sID) {
        super();
        this.sID = sID;
        rmiStack = new TopExecutionStack(getSID());
    }

    protected void createPausablesExecutor() {
        pausablesExecutor = ExecutorFactory.createRmiMirrorSyncService(this);
    }

    protected ContextAwarePendingRemoteObject(int port, ExecutionStack upStack, String sID, SyncType type) throws RemoteException {
        super(port, port >= 0);
        this.sID = sID;
        rmiStack = SyncExecutionStack.newThread(upStack, getSID(), type);
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }
    
    public boolean isLocal() { // means that object is not exported and used only at server
        return exportPort < 0;
    }

    private Set<Thread> getContextThreads() {
        assert !isLocal();
        synchronized (threads) {
            return threads.copy();
        }
    }
    
    public String getRemoteActionMessage() throws RemoteException {
        return ExecutionStackAspect.getActionMessage(getContextThreads());
    }

    public List<Object> getRemoteActionMessageList() throws RemoteException {
        return ExecutionStackAspect.getMessageList(getContextThreads());
    }

    public void addContextThread(Thread thread) {
        assert !isLocal();
        synchronized (threads) {
            threads.add(thread);
        }
    }

    public void removeContextThread(Thread thread) {
        assert !isLocal();
        synchronized (threads) {
            threads.remove(thread);
        }
    }

    public String getSID() {
        return sID;
    }
    protected boolean isEnabledUnreferenced() {
        return true;
    }

    @Override
    public void unreferenced() {
        if(!isEnabledUnreferenced())
            return;

        ThreadInfo threadInfo = EventThreadInfo.UNREFERENCED(this);
        ThreadLocalContext.aspectBeforeRmi(this, true, threadInfo);
        try {
            ServerLoggers.remoteLifeLog("REMOTE OBJECT UNREFERENCED " + this);

            deactivateAndCloseLater(true); // if it's unreferenced no more requests can come from clients, so we'll consider it confirmed
        } finally {
            ThreadLocalContext.aspectAfterRmi(threadInfo);
        }
    }

    // явная очистка ресурсов, которые поддерживаются через weak ref'ы
    protected void onClose() {
    }

    public boolean isDeactivated() {
        return deactivated;
    }
    public boolean isDeactivating() {
        return deactivating;
    }

    private boolean deactivated = false;
    private boolean deactivating = false;
    // умертвляет объект - отключает его от новых потоков + закрывает все старые потоки
    // ВАЖНО что должно выполняться в потоке, который сам не попадает в cleanThreads
    public synchronized void deactivate() {
        assert !isLocal();
        if(deactivated)
            return;

        ServerLoggers.remoteLifeLog("REMOTE OBJECT DEACTIVATE " + this);
        deactivating = true;

        onDeactivate();

        deactivated = true;
    }

    protected void onDeactivate() {
        unexport();

        if (pausablesExecutor != null)
            pausablesExecutor.shutdown();

        synchronized (threads) {
            for (Thread thread : threads) {
                ServerLoggers.exinfoLog("FORCEFULLY STOPPED : " + thread + '\n' + ExceptionUtils.getStackTrace() + '\n' + ExceptionUtils.getStackTrace(thread));
                try {
                    ThreadUtils.interruptThread(context, thread);
                } catch (SQLException | SQLHandledException ignored) {
                    ServerLoggers.sqlSuppLog(ignored);
                } catch (Throwable t) {
                    ServerLoggers.remoteLogger.error("onDeactivate: ", t);
                }
            }
        }
    }

    public void close() throws RemoteException { // client confirmed close
        deactivateAndCloseLater(true);
    }

    public void deactivateAndCloseLater(final boolean confirmedClient) {
        scheduleClose(confirmedClient ? Settings.get().getCloseConfirmedDelay() : Settings.get().getCloseNotConfirmedDelay(), () -> {
            try {
                deactivate();  // it's important to call it not in remote call context, otherwise it will deactivate itself
            } finally {
                scheduleClose(Settings.get().getCloseConfirmedDelay(), this::explicitClose); // give some more time deactivate (interrupt)
            }
        });
    }

    private boolean closed;
    public synchronized void explicitClose() {
        assert !isLocal();
        ServerLoggers.assertLog(deactivated, "REMOTE OBJECT MUST BE DEACTIVATED " + this);
        if(closed)
            return;

        ServerLoggers.remoteLifeLog("REMOTE OBJECT CLOSE " + this);

        onClose();

        closed = true;
    }
    
    public void localClose() {
        assert isLocal();
        
        // it makes no sense to call deactivate since all remote semantics is not used
        onClose();
    }

    public String toString() { // чтобы избегать ситуации когда включается log, toString падает по ошибке, а в месте log'а exception'ы не предполагаются (например dgc log, где поток checkLeases просто останавливается) 
        try {
            return notSafeToString();
        } catch (Throwable t) {
            return getDefaultToString(); 
        }
    }
    private String getDefaultToString() {
        return BaseUtils.defaultToString(this);
    }
    protected String notSafeToString() {
        return getDefaultToString();
    }

    public boolean isClosed() {
        return closed;
    }

    public void interrupt(boolean cancelable) throws RemoteException {
        try {
            Thread thread = ExecutionStackAspect.getLastThread(getContextThreads());
            if (thread != null) {
                Context context = getContext();
                if (cancelable)
                    ThreadUtils.cancelThread(context, thread);
                else
                    ThreadUtils.interruptThread(context, thread);
            }
        } catch (SQLException | SQLHandledException ignored) {
        }
    }
}
