package platform.client;

import platform.client.rmi.ConnectionLostManager;
import platform.interop.remote.ClientCallbackInterface;

import javax.swing.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ClientCallBack extends UnicastRemoteObject implements ClientCallbackInterface {
    public ClientCallBack() throws RemoteException {
    }

    public void disconnect() throws RemoteException {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ConnectionLostManager.forceDisconnect();
            }
        });
    }

    public void notifyServerRestart() throws RemoteException {
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
                        Main.remoteNavigator.denyRestart();
                    } catch (RemoteException e) {
                        throw new RuntimeException("Ошибка при посыле отказа от остановки сервера.", e);
                    }
                }
            }
        });
    }
}
