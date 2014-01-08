package lsfusion.client.exceptions;

import lsfusion.base.ExceptionUtils;
import lsfusion.client.Log;
import lsfusion.client.Main;
import lsfusion.client.rmi.ConnectionLostManager;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.rmi.RemoteException;
import java.rmi.ServerException;
import java.util.*;

public class ClientExceptionManager {
    private final static Logger logger = Logger.getLogger(ClientExceptionManager.class);
    private final static List<Throwable> unreportedThrowables = Collections.synchronizedList(new ArrayList<Throwable>());

    public static void handle(final Throwable e) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                logger.error("Client error: ", e);

                RemoteException remote = getRemoteExceptionCause(e);

                if (remote != null && !(remote instanceof ServerException)) {
                    if (ExceptionUtils.isFatalRemoteException(remote)) {
                        ConnectionLostManager.connectionLost();
                    } else {
                        ConnectionLostManager.connectionBroke();
                    }
                }

                String stackTrace = ExceptionUtils.getStackTraceString(e);

                if (!(e instanceof ConcurrentModificationException && stackTrace.contains("bibliothek.gui.dock.themes.basic.action.buttons.ButtonPanel.setForeground"))) {
                    try {
                        Main.clientExceptionLog("Client error", e);
                    } catch (Throwable ex) {
                        unreportedThrowables.add(e);
                        logger.error("Error reporting client exception: " + e, ex);
                    }
                    if (remote == null) {
                        Log.error("Внутренняя ошибка сервера", stackTrace);
                    }
                }
            }
        });
    }

    private static RemoteException getRemoteExceptionCause(Throwable e) {
        for (Throwable ex = e; ex != null && ex != ex.getCause(); ex = ex.getCause()) {
            if (ex instanceof RemoteException) {
                return (RemoteException) ex;
            }
        }
        return null;
    }

    public static void flushUnreportedThrowables() {
        synchronized (unreportedThrowables) {
            for (Iterator<Throwable> iterator = unreportedThrowables.iterator(); iterator.hasNext(); ) {
                Throwable t = iterator.next();
                try {
                    Main.clientExceptionLog("Unreported client error", t);
                    iterator.remove();
                } catch (Throwable e) {
                    logger.error("Error reporting unreported client exception: " + t, e);
                }
            }
        }
    }
}
