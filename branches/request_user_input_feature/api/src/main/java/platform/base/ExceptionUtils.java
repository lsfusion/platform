package platform.base;

import com.google.common.base.Throwables;
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

    public static RemoteException propogateRemoteException(Throwable t) throws RemoteException {
        Throwables.propagateIfPossible(t, RemoteException.class);
        throw Throwables.propagate(t);
    }

    public static void dumpStack() {
        new Exception("Stack trace").printStackTrace(System.out);
    }
}
