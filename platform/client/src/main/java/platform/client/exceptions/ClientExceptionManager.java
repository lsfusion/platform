package platform.client.exceptions;

import platform.client.Log;
import platform.client.rmi.ConnectionLostManager;
import platform.interop.exceptions.InternalServerException;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.rmi.ConnectException;
import java.rmi.ConnectIOException;
import java.util.ConcurrentModificationException;

public class ClientExceptionManager {

    public static void handle(Throwable e) {
        handle(e, null);
    }

    public static void handle(Throwable e, Component parentComponent) {

        // Проверяем на потерю соединения и делаем особую обработку
        for (Throwable ex = e; ex != null; ex = ex.getCause()) {
            if (ex instanceof ConnectIOException || ex instanceof ConnectException) {
                ConnectionLostManager.setConnectionLost(true);
                return;
            }
            if (ex == ex.getCause()) {
                break;
            }
        }

        String message = e.getLocalizedMessage();
        String trace = "";

        if (e.getCause() instanceof InternalServerException) {
            trace = ((InternalServerException) e.getCause()).trace;
        } else {
            while (e instanceof RuntimeException && e.getCause() != null) {
                e = e.getCause();
                message += " : \n" + e.getLocalizedMessage();
            }
        }

        //message += "\n-----------\n";
        OutputStream os = new ByteArrayOutputStream();
        e.printStackTrace(new PrintStream(os));
        String erTrace = os.toString();

        if (!(e instanceof ConcurrentModificationException) ||
                !(erTrace.indexOf("bibliothek.gui.dock.themes.basic.action.buttons.ButtonPanel.setForeground(Unknown Source)") >= 0)) {
            Log.printFailedMessage(message, trace, parentComponent);
        }

        e.printStackTrace();
    }

}
