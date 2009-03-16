package platform.client.exceptions;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import platform.client.Log;
import platform.interop.exceptions.RemoteServerException;

class ClientExceptionManager {

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
