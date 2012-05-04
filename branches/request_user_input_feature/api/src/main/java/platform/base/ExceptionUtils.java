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

    public static boolean isRecoverableRemoteException(Throwable remote) {
        return remote instanceof RemoteException &&
               (remote.getClass() == RemoteException.class
                || remote instanceof ServerException
                || getInitialCause(remote) instanceof RemoteServerException);
    }

    public static Throwable getNonSpringCause(Throwable throwable) {
        Throwable result = throwable;
        while (result != null && result.getCause() != null && result instanceof org.springframework.beans.BeansException) {
            result = result.getCause();
        }

        return result;
    }

    public static void emitRemoteException(Throwable t) throws RemoteException {
        if (t == null) {
            throw new RuntimeException("Internal error: null");
        } else if (t instanceof RemoteException) {
            throw (RemoteException)t;
        } else if (t instanceof RuntimeException) {
            throw (RuntimeException)t;
        }

        throw new RuntimeException(t);
    }
}
