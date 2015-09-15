package lsfusion.server.remote;

import lsfusion.base.ExceptionUtils;
import lsfusion.interop.remote.PendingRemoteObject;
import lsfusion.server.ServerLoggers;
import lsfusion.server.context.Context;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ThreadUtils;

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ContextAwarePendingRemoteObject extends PendingRemoteObject {

    private final List<Thread> threads = Collections.synchronizedList(new ArrayList<Thread>());

    protected Context context;

    protected ContextAwarePendingRemoteObject() {
        super();
    }

    protected ContextAwarePendingRemoteObject(int port) throws RemoteException {
        super(port);
    }

    protected ContextAwarePendingRemoteObject(int port, boolean autoExport) throws RemoteException {
        super(port, autoExport);
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

    public void addLinkedThread(Thread thread) {
        threads.add(thread);
    }

    public void removeLinkedThread(Thread thread) {
        threads.remove(thread);
    }

    @Override
    public void unexportAndClean() {
            synchronized (threads) {
                for (Thread thread : threads) {
                    ServerLoggers.exinfoLog("FORCEFULLY STOPPED : " + thread + '\n' + ExceptionUtils.getStackTrace(thread.getStackTrace()));
                    try {
                    ThreadUtils.interruptThread(context, thread);
                    } catch (SQLException | SQLHandledException ignored) {
                    }
                }
            }
            super.unexportAndClean();
    }
}
