package platform.client.exceptions;

import org.apache.log4j.Logger;
import platform.base.SystemUtils;
import platform.client.ClientResourceBundle;
import platform.client.Log;
import platform.client.Main;
import platform.client.rmi.ConnectionLostManager;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.rmi.ConnectException;
import java.rmi.ConnectIOException;
import java.rmi.RemoteException;
import java.rmi.ServerException;
import java.util.ConcurrentModificationException;

import static platform.base.BaseUtils.lineSeparator;

public class ClientExceptionManager {
    private final static Logger logger = Logger.getLogger(ClientExceptionManager.class);

    public static void handle(Throwable e) {
        // Проверяем на потерю соединения и делаем особую обработку
        RemoteException remote = getRemoteExceptionCause(e);
        if (remote != null) {
            handleRemoteException(e, remote);
        } else {
            handleClientException(e);
        }
    }

    private static void handleRemoteException(Throwable initial, RemoteException remote) {
        if (remote instanceof ConnectIOException || remote instanceof ConnectException) {
            //при этих RemoteException'ах возможно продолжение работы
            ConnectionLostManager.connectionLost(false);
        } else {
            //при остальных RemoteException'ах нужно релогиниться
            ConnectionLostManager.connectionLost(true);
        }
        logger.error(ClientResourceBundle.getString("exceptions.error.on.communication.with.server"), initial);
    }

    private static void handleClientException(Throwable e) {
        //здесь обрабатываются и все RemoteServerException
        logger.error("Client error: ", e);

        String message = e.getLocalizedMessage();
        while (e instanceof RuntimeException && e.getCause() != null) {
            e = e.getCause();
            message += " : " + lineSeparator + e.getLocalizedMessage();
        }

        OutputStream stackStream = new ByteArrayOutputStream();
        e.printStackTrace(new PrintStream(stackStream));
        String stackTrace = stackStream.toString();

        if (!(e instanceof ConcurrentModificationException && stackTrace.contains("bibliothek.gui.dock.themes.basic.action.buttons.ButtonPanel.setForeground"))) {
            try {
                String info = ClientResourceBundle.getString("exceptions.client.error", SystemUtils.getLocalHostName(), message) + lineSeparator + stackTrace;
                Main.clientExceptionLog(info, SystemUtils.getLocalHostName(), message, e.getClass().getName(), stackTrace);
            } catch (RemoteException ignored) {
            }
            Log.printFailedMessage(ClientResourceBundle.getString("exceptions.error.on.executing") + message, stackTrace);
        }
    }

    private static RemoteException getRemoteExceptionCause(Throwable e) {
        for (Throwable ex = e; ex != null && ex != ex.getCause(); ex = ex.getCause()) {
            if (ex instanceof RemoteException) {
                return ex instanceof ServerException
                       ? (RemoteException) ex.getCause()
                       : (RemoteException) ex;
            }
        }
        return null;
    }

}
