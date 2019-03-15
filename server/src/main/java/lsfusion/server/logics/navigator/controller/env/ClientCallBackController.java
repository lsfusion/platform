package lsfusion.server.logics.navigator.controller.env;

import lsfusion.interop.navigator.callback.ClientCallBackInterface;
import lsfusion.interop.navigator.callback.LifecycleMessage;
import lsfusion.interop.navigator.callback.PushMessage;
import lsfusion.server.physics.admin.logging.ServerLoggers;
import lsfusion.server.base.controller.remote.RemoteObject;

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
