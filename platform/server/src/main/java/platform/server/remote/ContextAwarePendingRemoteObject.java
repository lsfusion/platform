package platform.server.remote;

import platform.interop.remote.PendingRemoteObject;
import platform.server.Settings;
import platform.server.context.Context;
import platform.server.context.ThreadLocalContext;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public abstract class ContextAwarePendingRemoteObject extends PendingRemoteObject {

    public final List<Thread> threads = new ArrayList<Thread>();

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

    public void killThreads() {
        if (Settings.get().getKillThread())
            for (Thread thread : threads) {
                thread.stop();
            }
    }
}
