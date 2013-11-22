package lsfusion.client.exceptions;

import lsfusion.base.ExceptionUtils;
import lsfusion.base.SystemUtils;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.Log;
import lsfusion.client.Main;
import lsfusion.client.rmi.ConnectionLostManager;
import org.apache.log4j.Logger;

import java.rmi.ConnectException;
import java.rmi.ConnectIOException;
import java.rmi.RemoteException;
import java.rmi.ServerException;
import java.util.*;

import static lsfusion.base.BaseUtils.lineSeparator;

public class ClientExceptionManager {
    private final static Logger logger = Logger.getLogger(ClientExceptionManager.class);
    private final static List<Throwable> unreportedThrowables = Collections.synchronizedList(new ArrayList<Throwable>());

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
        logger.error(ClientResourceBundle.getString("exceptions.error.on.communication.with.server"), initial);
        unreportedThrowables.add(initial);
        if (remote instanceof ConnectIOException || remote instanceof ConnectException) {
            //при этих RemoteException'ах возможно продолжение работы
            ConnectionLostManager.connectionLost(false);
        } else {
            //при остальных RemoteException'ах нужно релогиниться
            ConnectionLostManager.connectionLost(true);
        }
    }

    private static void handleClientException(Throwable e) {
        //здесь обрабатываются и все RemoteServerException
        logger.error("Client error: ", e);

        String message = e.getLocalizedMessage();
        while (e instanceof RuntimeException && e.getCause() != null) {
            e = e.getCause();
            message += " : " + lineSeparator + e.getLocalizedMessage();
        }

        String stackTrace = ExceptionUtils.getStackTraceString(e);

        if (!(e instanceof ConcurrentModificationException && stackTrace.contains("bibliothek.gui.dock.themes.basic.action.buttons.ButtonPanel.setForeground"))) {
            try {
                String info = ClientResourceBundle.getString("exceptions.client.error", SystemUtils.getLocalHostName(), message) + lineSeparator + stackTrace;
                Main.clientExceptionLog(info, SystemUtils.getLocalHostName(), message, e.getClass().getName(), stackTrace);
            } catch (RemoteException ignored) {
                logger.error("Internal sending exception on server: ", e);
            }
            Log.error("Внутренняя ошибка сервера", stackTrace);
        }
    }

    public static void flushUnreportedThrowables() {
        synchronized (unreportedThrowables) {
            for (Iterator<Throwable> iterator = unreportedThrowables.iterator(); iterator.hasNext(); ) {
                Throwable t = iterator.next();
                try {
                    Main.clientExceptionLog("Connection lost remote exception", SystemUtils.getLocalHostName(), t.getMessage(), t.getClass().getName(), ExceptionUtils.getStackTraceString(t));
                    iterator.remove();
                } catch (Throwable e) {
                    logger.error("Error reporting unreported exception: " + t, e);
                }
            }
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
