package platform.client.exceptions;

import platform.client.Log;

public class ClientExceptionManager {

    public static void handleException(Throwable e) {

        String message = e.getLocalizedMessage();
        while (e instanceof RuntimeException && e.getCause() != null) {
            e = e.getCause();
            message += " : \n" + e.getLocalizedMessage();
        }
        Log.printFailedMessage(message);

        e.printStackTrace();
    }

}
