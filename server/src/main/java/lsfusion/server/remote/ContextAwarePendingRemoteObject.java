package lsfusion.server.remote;

import lsfusion.base.BaseUtils;
import lsfusion.base.ExceptionUtils;
import lsfusion.base.WeakIdentityHashSet;
import lsfusion.interop.exceptions.RemoteInternalException;
import lsfusion.interop.remote.PendingRemoteObject;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.context.*;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.ThreadUtils;
import lsfusion.server.profiler.ProfiledObject;
import lsfusion.server.stack.ExecutionStackAspect;
import org.apache.log4j.Logger;

import java.rmi.RemoteException;
import java.rmi.server.Unreferenced;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

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

    protected boolean isUnreferencedSyncedClient() {
        return false;
    }

    @Override
    public void unreferenced() {
        if(!isEnabledUnreferenced())
            return;

        ThreadInfo threadInfo = EventThreadInfo.UNREFERENCED(this);
        ThreadLocalContext.aspectBeforeRmi(this, true, threadInfo);
        try {
            ServerLoggers.remoteLifeLog("REMOTE OBJECT UNREFERENCED " + this);

            deactivateAndCloseLater(isUnreferencedSyncedClient());
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
                ThreadInfo threadInfo = EventThreadInfo.TIMER(ContextAwarePendingRemoteObject.this);
                ThreadLocalContext.aspectBeforeRmi(ContextAwarePendingRemoteObject.this, true, threadInfo);
                try {
                    deactivate();

                    try {
                        Thread.sleep(delay); // даем время на deactivate (interrupt)
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    explicitClose();
                } finally {
                    ThreadLocalContext.aspectAfterRmi(threadInfo);
                }
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

    public void logServerException(RemoteInternalException t) throws SQLException, SQLHandledException {
        BusinessLogics businessLogics = getContext().getLogicsInstance().getBusinessLogics();
        businessLogics.systemEventsLM.logException(businessLogics, getStack(), t, null, null, false, false);
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
