package platform.client;

import platform.client.rmi.ConnectionLostManager;
import platform.interop.remote.CallbackMessage;
import platform.interop.remote.ClientCallBackInterface;

import javax.swing.*;
import java.rmi.RemoteException;
import java.util.List;

import static platform.client.ClientResourceBundle.getString;

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
            case SERVER_RESTARTING:
                notifyServerRestarting();
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

    public void notifyServerRestarting() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                int result = JOptionPane.showConfirmDialog(
                        SwingUtils.getActiveWindow(),getString("notification.server.stop"),
                        getString("notification.stop.title"),
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (result == JOptionPane.CANCEL_OPTION) {
                    try {
                        remoteClient.denyRestart();
                    } catch (RemoteException e) {
                        throw new RuntimeException(getString("notification.error.cancelstopping"), e);
                    }
                }
            }
        });
    }
}
