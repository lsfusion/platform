package lsfusion.server.form.navigator;

import lsfusion.interop.remote.*;
import lsfusion.server.ServerLoggers;

import java.rmi.RemoteException;
import java.rmi.server.Unreferenced;
import java.util.ArrayList;
import java.util.List;

public class ClientCallBackController extends RemoteObject implements ClientCallBackInterface, Unreferenced {
    public interface UsageTracker {
        void used();
    }

    private final List<LifecycleMessage> messages = new ArrayList<>();
    private final UsageTracker usageTracker;

    public ClientCallBackController(int port, String caption, UsageTracker usageTracker) throws RemoteException {
        super(port, true);
        this.caption = caption;
        this.usageTracker = usageTracker;
    }

    public synchronized void pushMessage(Integer idNotification) {
        messages.add(new PushMessage(idNotification));
    }

    public synchronized List<LifecycleMessage> pullMessages() {
        if (usageTracker != null) {
            usageTracker.used();
        }
        ArrayList result = new ArrayList(messages);
        messages.clear();
        return result.isEmpty() ? null : result;
    }

    private String caption;
    public void unreferenced() {
        ServerLoggers.remoteLifeLog("CALLBACK UNREFERENCED : " + caption);
    }
}
