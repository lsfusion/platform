package platform.client;

import platform.client.rmi.ConnectionLostManager;
import platform.interop.remote.CallbackMessage;
import platform.interop.remote.ClientCallBackInterface;

import javax.swing.*;
import java.rmi.RemoteException;
import java.util.List;

public class ClientCallBackProcessor {
    private final ClientCallBackInterface remoteClient;

    public ClientCallBackProcessor(ClientCallBackInterface remoteClient) {
        this.remoteClient = remoteClient;
    }

    public void processMessages(List<CallbackMessage> messages) {
        if (messages != null) {
            for (CallbackMessage message : messages) {
                processMessage(message);
            }
        }
    }

    private void processMessage(CallbackMessage message) {
        switch (message) {
            case DISCONNECTED:
                disconnect();
                break;
            case SERVER_RESTARTED:
                notifyServerRestart();
                break;
        }
    }

    public void disconnect() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ConnectionLostManager.forceDisconnect();
            }
        });
    }

    public void notifyServerRestart() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                int result = JOptionPane.showConfirmDialog(
                        SwingUtils.getActiveWindow(),
                            "<html>"+ClientResourceBundle.getString("notification.server.will.be.stopped")+"<br/>" +
                            ClientResourceBundle.getString("notification.save.and.exit")+"<br/>" +
                            ClientResourceBundle.getString("notification.cancel.if.can.not.break")+"</html>",
                        ClientResourceBundle.getString("notification.stop.server"),
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (result == JOptionPane.CANCEL_OPTION) {
                    try {
                        remoteClient.denyRestart();
                    } catch (RemoteException e) {
                        throw new RuntimeException(ClientResourceBundle.getString("notification.error.cancelling.stopping.server"), e);
                    }
                }
            }
        });
    }
}
