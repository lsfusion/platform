package platform.client.exceptions;

import org.apache.log4j.Logger;
import platform.base.OSUtils;
import platform.client.ClientResourceBundle;
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
import java.rmi.ServerException;
import java.util.ConcurrentModificationException;

import static platform.base.BaseUtils.lineSeparator;

public class ClientExceptionManager {
    private final static Logger logger = Logger.getLogger(ClientExceptionManager.class);

    public static void handle(Throwable e) {
        handle(e, null);
    }

    public static void handle(Throwable e, Component parentComponent) {

        // Проверяем на потерю соединения и делаем особую обработку
        RemoteException remote = getRemoteExceptionCause(e);
        if (remote instanceof ConnectIOException || remote instanceof ConnectException) {
            //при этих RemoteException'ах возможно продолжение работы
            ConnectionLostManager.connectionLost(false);
            logger.error(ClientResourceBundle.getString("exceptions.error.on.communication.with.server")+": ", e);
            return;
        }

        boolean isInternalServerException = false;
        if (remote != null) {
            // если не подкласс, а сам эксешн, то не считаем фатальным...
            if (remote.getClass() == RemoteException.class || remote.getCause() instanceof InternalServerException) {
                isInternalServerException = true;
            } else {
                //при остальных RemoteException'ах нужно релогиниться
                ConnectionLostManager.connectionLost(true);
                logger.error(ClientResourceBundle.getString("exceptions.error.on.communication.with.server")+": ", e);
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

        e.printStackTrace();

        OutputStream os = new ByteArrayOutputStream();
        e.printStackTrace(new PrintStream(os));
        String erTrace = os.toString();

        if (!(e instanceof ConcurrentModificationException) ||
            !(erTrace.indexOf("bibliothek.gui.dock.themes.basic.action.buttons.ButtonPanel.setForeground") >= 0)) {
            try {
                String info = ClientResourceBundle.getString("exceptions.client")+" " + OSUtils.getLocalHostName() + ClientResourceBundle.getString("exceptions.error") +" " + message;
                if (!isInternalServerException) {
                    info += lineSeparator + erTrace;
                }
                Main.frame.remoteNavigator.clientExceptionLog(info);
            } catch (RemoteException exc) {
            }
            Log.printFailedMessage(ClientResourceBundle.getString("exceptions.error.on.executing")+" : " + message, erTrace, parentComponent);
        }
    }

    private static RemoteException getRemoteExceptionCause(Throwable e) {
        for (Throwable ex = e; ex != null && ex.getCause() != null && ex != ex.getCause(); ex = ex.getCause()) {
            if (ex instanceof RemoteException) {
                return ex instanceof ServerException
                       ? (RemoteException) ex.getCause()
                       : (RemoteException) ex;
            }
        }
        return null;
    }

}
