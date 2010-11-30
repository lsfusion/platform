package platform.client.exceptions;

import platform.base.OSUtils;
import platform.client.Log;
import platform.client.Main;
import platform.client.rmi.ConnectionLostManager;
import platform.interop.exceptions.InternalServerException;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.rmi.ConnectException;
import java.rmi.ConnectIOException;
import java.rmi.RemoteException;
import java.util.ConcurrentModificationException;
import static platform.base.BaseUtils.lineSeparator;

public class ClientExceptionManager {

    public static void handle(Throwable e) {
        handle(e, null);
    }

    public static void handle(Throwable e, Component parentComponent) {

        // Проверяем на потерю соединения и делаем особую обработку
        RemoteException remote = getRemoteExceptionCause(e);
        if (remote instanceof ConnectIOException || remote instanceof ConnectException) {
            //при этих RemoteException'ах возможно продолжение работы
            ConnectionLostManager.connectionLost(false);
            return;
        }

        boolean isInternalServerException = false;
        if (remote != null) {
            if (remote.getCause() instanceof InternalServerException) {
                isInternalServerException = true;
            } else {
                //при остальных RemoteException'ах нужно релогиниться
                ConnectionLostManager.connectionLost(true);
                return;
            }
        }

        String message = e.getLocalizedMessage();
        if (!isInternalServerException) {
            while (e instanceof RuntimeException && e.getCause() != null) {
                e = e.getCause();
                message += " : " + lineSeparator + e.getLocalizedMessage();
            }
        }

        OutputStream os = new ByteArrayOutputStream();
        e.printStackTrace(new PrintStream(os));
        String erTrace = os.toString();

        if (!(e instanceof ConcurrentModificationException) ||
                !(erTrace.indexOf("bibliothek.gui.dock.themes.basic.action.buttons.ButtonPanel.setForeground(Unknown Source)") >= 0)) {
            try {
                String info = "Клиент: " + OSUtils.getLocalHostName() + ",Ошибка: " + message;
                if (!isInternalServerException) {
                    info += lineSeparator + erTrace;
                }
                Main.frame.remoteNavigator.clientExceptionLog(info);
            } catch (RemoteException exc) {
            }
            Log.printFailedMessage("Произошла ошибка во время выполнения : " + message, erTrace, parentComponent);
        }

        e.printStackTrace();
    }

    private static RemoteException getRemoteExceptionCause(Throwable e) {
        for (Throwable ex = e; ex != null && ex != ex.getCause(); ex = ex.getCause()) {
            if (ex instanceof RemoteException) {
                return (RemoteException) ex;
            }
        }
        return null;
    }

}
