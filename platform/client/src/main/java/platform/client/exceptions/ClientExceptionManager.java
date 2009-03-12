package platform.client.exceptions;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import platform.client.Log;
import platform.interop.exceptions.RemoteServerException;

@Aspect
public class ClientExceptionManager {

    @Before("handler(Exception) && args(e)")
    public void runExceptionHandler(Exception e) {

        handleException("Ошибка в клиентском приложении", e);
    }

    public static void handleException(String message, Throwable e) {

        message += " : " + e.getLocalizedMessage();
        Log.printFailedMessage(message);
    }

}
