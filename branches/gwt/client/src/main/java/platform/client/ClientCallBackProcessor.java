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
                        Main.frame,
                            "<html>Сервер будет остановлен через 15 минут!.<br/>" +
                            "Сохраните текущую работу и выйдите из приложения.<br/>" +
                            "Если вы выполняете работу, которая не может быть прервана, нажмите отмену.</html>",
                        "Остановка сервера",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (result == JOptionPane.CANCEL_OPTION) {
                    try {
                        remoteClient.denyRestart();
                    } catch (RemoteException e) {
                        throw new RuntimeException("Ошибка при посыле отказа от остановки сервера.", e);
                    }
                }
            }
        });
    }
}
