package platform.server.form.navigator;

import platform.interop.remote.CallbackMessage;
import platform.interop.remote.ClientCallBackInterface;
import platform.interop.remote.RemoteObject;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class ClientCallBackController extends RemoteObject implements ClientCallBackInterface {
    private List<CallbackMessage> messages = new ArrayList<CallbackMessage>();
    private Boolean deniedRestart = null;

    public ClientCallBackController(int port) throws RemoteException {
        super(port);
    }

    public synchronized void disconnect() {
        addMessage(CallbackMessage.DISCONNECTED);
    }

    public synchronized void cutOff() {
        addMessage(CallbackMessage.CUT_OFF);
    }

    public synchronized void notifyServerRestart() {
        deniedRestart = false;
        addMessage(CallbackMessage.SERVER_RESTARTING);
    }

    public synchronized void notifyServerRestartCanceled() {
        deniedRestart = null;
    }

    public synchronized void denyRestart() {
        deniedRestart = true;
    }

    public synchronized boolean isRestartAllowed() {
        //если не спрашивали, либо если отказался
        return deniedRestart != null && !deniedRestart;
    }

    public synchronized void addMessage(CallbackMessage message) {
        messages.add(message);
    }

    public synchronized List<CallbackMessage> pullMessages() {
        ArrayList result = new ArrayList(messages);
        messages.clear();
        return result.isEmpty() ? null : result;
    }
}
