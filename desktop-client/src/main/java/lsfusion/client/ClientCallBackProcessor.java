package lsfusion.client;

import lsfusion.client.rmi.ConnectionLostManager;
import lsfusion.interop.remote.ClientCallBackInterface;
import lsfusion.interop.remote.ClientCallbackMessage;
import lsfusion.interop.remote.LifecycleMessage;
import lsfusion.interop.remote.PushMessage;

import javax.swing.*;
import java.rmi.RemoteException;
import java.util.List;

import static lsfusion.client.ClientResourceBundle.getString;

public class ClientCallBackProcessor {
    private final ClientCallBackInterface remoteClient;

    public ClientCallBackProcessor(ClientCallBackInterface remoteClient) {
        this.remoteClient = remoteClient;
    }

    public void processMessages(List<LifecycleMessage> messages) {
        if (messages != null) {
            for (LifecycleMessage message : messages) {
                processMessage(message);
            }
        }
    }

    private void processMessage(final LifecycleMessage message) {
        if(message instanceof ClientCallbackMessage) {
            switch (((ClientCallbackMessage) message).message) {
                case DISCONNECTED:
                    Main.closeHangingSockets();
                    disconnect(getString("rmi.connectionlost.disconnect"));
                    break;
                case SERVER_RESTARTING:
                    notifyServerRestarting();
                    break;
                case CLIENT_RESTART:
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            Main.reconnect();
                        }
                    });
                    Main.reconnect();
                    break;
                case CLIENT_SHUTDOWN:
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            Main.shutdown();
                        }
                    });
                    break;
            }
        } else if(message instanceof PushMessage) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    Main.executeNotificationAction(((PushMessage)message).idNotification);
                }
            });
        }
    }

    public void disconnect(final String message) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ConnectionLostManager.forceDisconnect(message);
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
