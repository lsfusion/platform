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

    public void closeLater() {
        BaseUtils.runLater(Settings.get().getCloseFormDelay(), new Runnable() { // тут надо бы на ContextAwareDaemonThreadFactory переделать
            @Override
            public void run() {
                ThreadLocalContext.aspectBeforeRmi(ContextAwarePendingRemoteObject.this, true);
                try {
                    explicitClose();
                } finally {
                    ThreadLocalContext.aspectAfterRmi();
                }
            }
        });
    }

    public abstract String getSID();

    public void explicitClose() { // потом надо переминовать в close, но тогда close надо переименовывать и повышать версию интерфейса
        shutdown(true);
    }

    @Override
    public void unreferenced() {
        ThreadLocalContext.aspectBeforeRmi(this, true);
        try {
            if(!Settings.get().isDisableUnreferenced())
                shutdown(true);
        } finally {
            ThreadLocalContext.aspectAfterRmi();
        }
    }

    // явная очистка ресурсов, которые поддерживаются через weak ref'ы
    protected void onExplicitClose() {
        unexport();

        BaseUtils.runLater(Settings.get().getCloseFormDelay(), cleanThreads(true, threads, context));
    }

    // все кроме weakRef (onExplicitClose) !!!! ВАЖНО нельзя запускать очистку weakRef ресурсов, так как WeakReference'у уже могут стать null, и ресурсы (например временные таблицы) перейдут другому владельцу, в итоге почистятся ресурсы используемые уже новым объектом
    protected void onFinalClose(final boolean explicit) { // assert synchronized
        if (pausablesExecutor != null)
            pausablesExecutor.shutdown();
    }

    private static Runnable cleanThreads(final boolean explicit, final WeakIdentityHashSet<Thread> threads, final Context context) {
        return new Runnable() {
            @Override
            public void run() {
                synchronized (threads) {
                    for (Thread thread : threads) {
                        ServerLoggers.exinfoLog("FORCEFULLY STOPPED : " + thread + '\n' + ExceptionUtils.getStackTrace() + '\n' + ExceptionUtils.getStackTrace(thread.getStackTrace()));
                        try {
                            ThreadUtils.interruptThread(context, thread);
                        } catch (SQLException | SQLHandledException ignored) {
                            ServerLoggers.sqlSuppLog(ignored);
                        }
                    }
                }
            }
        };
    }

    private boolean closed;
    private synchronized void shutdown(boolean explicit) {  // по идее assert synchronized но может быть проблема так как unreferenced и explicitClose могут быть вместе
        if(closed) {
//            if (explicit) // много вариантов когда закрывается несколько раз explicit, unreferenced + close, * + forceDisconnect
//                ServerLoggers.assertLog(false, "REMOTE OBJECT ALREADY CLOSED " + this);
            return;
        }
        ServerLoggers.remoteLifeLog("REMOTE OBJECT CLOSE " + this);
        if (explicit)
            onExplicitClose();
        onFinalClose(explicit);
        closed = true;
    }

    public boolean isClosed() {
        return closed;
    }

    protected void finalize() throws Throwable {
        ThreadLocalContext.aspectBeforeRmi(this, true);
        try {
            try {
                if(!Settings.get().isDisableFinalized())
                    shutdown(false);
            } catch (Throwable ignored) {
            } finally {
                super.finalize();
            }
        } finally {
            ThreadLocalContext.aspectAfterRmi();
        }
    }

    public void logServerException(Throwable t) throws SQLException, SQLHandledException {
        BusinessLogics businessLogics = getContext().getLogicsInstance().getBusinessLogics();
        businessLogics.systemEventsLM.logException(businessLogics, getStack(), t, null, null, false);
    }
}
