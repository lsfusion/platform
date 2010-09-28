package platform.client.exceptions;

import platform.client.Log;
import platform.client.rmi.ConnectionLostManager;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.rmi.ConnectException;
import java.rmi.ConnectIOException;

public class ClientExceptionManager {

    public static void handleException(Throwable e) {
        handleException(e, null);
    }

    public static void handleException(Throwable e, Component parentComponent) {

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
        while (e instanceof RuntimeException && e.getCause() != null) {
            e = e.getCause();
            message += " : \n" + e.getLocalizedMessage();
        }

        message += "\n-----------\n";
        OutputStream os = new ByteArrayOutputStream();
        e.printStackTrace(new PrintStream(os));
        message += os.toString();

        Log.printFailedMessage(message.substring(0, 2000), parentComponent);

        e.printStackTrace();
    }

}
