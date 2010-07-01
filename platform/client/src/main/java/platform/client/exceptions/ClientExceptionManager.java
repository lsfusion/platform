package platform.client.exceptions;

import platform.client.Log;

import java.awt.*;

public class ClientExceptionManager {

    public static void handleException(Throwable e) {
        handleException(e, null);
    }

    public static void handleException(Throwable e, Component parentComponent) {

        String message = e.getLocalizedMessage();
        while (e instanceof RuntimeException && e.getCause() != null) {
            e = e.getCause();
            message += " : \n" + e.getLocalizedMessage();
        }
        Log.printFailedMessage(message, parentComponent);

        e.printStackTrace();
    }

}
