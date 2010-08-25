package platform.client.exceptions;

import platform.client.Log;
import platform.client.rmi.ConnectionLostManager;

import java.awt.*;
import java.rmi.ConnectException;
import java.rmi.ConnectIOException;

public class ClientExceptionManager {

    public static void handleException(Throwable e) {
        handleException(e, null);
    }

    public static void handleException(Throwable e, Component parentComponent) {

        // Проверяем на потерю соединения и делаем особую обработку
        while (e != null) {
            if (e instanceof ConnectIOException || e instanceof ConnectException) {
                ConnectionLostManager.setConnectionLost(true);
                return;
            }
            if (e == e.getCause()) {
                break;
            }
            e = e.getCause();
        }

        String message = e.getLocalizedMessage();
        while (e instanceof RuntimeException && e.getCause() != null) {
            e = e.getCause();
            message += " : \n" + e.getLocalizedMessage();
        }
        Log.printFailedMessage(message, parentComponent);

        e.printStackTrace();
    }

}
