package platform.base;

import platform.interop.exceptions.RemoteServerException;

import java.rmi.RemoteException;
import java.rmi.ServerException;

public class ExceptionUtils {
    public static Throwable getInitialCause(Throwable throwable) {
        Throwable result = throwable;
        while (result != null && result.getCause() != null && result instanceof RemoteException) {
            result = result.getCause();
        }

        return result;
    }

    public static boolean isRecoverableRemoteException(RemoteException remote) {
        return remote.getClass() == RemoteException.class
               || remote instanceof ServerException
               || getInitialCause(remote) instanceof RemoteServerException;
    }
}
