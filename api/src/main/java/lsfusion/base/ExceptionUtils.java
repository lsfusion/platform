package lsfusion.base;

import com.google.common.base.Throwables;
import lsfusion.interop.exceptions.RemoteServerException;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.rmi.*;

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

    public static RemoteException propagateRemoteException(Throwable t) throws RemoteException {
        throw propagate(t, RemoteException.class);
    }

    public static <E1 extends Throwable> RuntimeException propagate(Throwable t,
                                                                    Class<E1> declaredType) throws E1 {
        Throwables.propagateIfInstanceOf(t, declaredType);
        throw Throwables.propagate(t);
    }

    public static <E1 extends Throwable, E2 extends Throwable> RuntimeException propagate(Throwable t,
                                                                                          Class<E1> declaredType1,
                                                                                          Class<E2> declaredType2) throws E1, E2 {
        Throwables.propagateIfInstanceOf(t, declaredType1);
        Throwables.propagateIfInstanceOf(t, declaredType2);
        throw Throwables.propagate(t);
    }

    public static <E1 extends Throwable, E2 extends Throwable, E3 extends Throwable> RuntimeException propagate(Throwable t,
                                                                                                                Class<E1> declaredType1,
                                                                                                                Class<E1> declaredType2,
                                                                                                                Class<E2> declaredType3) throws E1, E2, E3 {
        Throwables.propagateIfInstanceOf(t, declaredType1);
        Throwables.propagateIfInstanceOf(t, declaredType2);
        Throwables.propagateIfInstanceOf(t, declaredType3);
        throw Throwables.propagate(t);
    }

    public static void dumpStack() {
        new Exception("Stack trace").printStackTrace(System.out);
    }

    public static String getStackTraceString(Throwable t) {
        ByteArrayOutputStream stackStream = new ByteArrayOutputStream();
        t.printStackTrace(new PrintStream(stackStream));
        return stackStream.toString();
    }

    public static boolean isFatalRemoteException(Throwable t) {
        return t instanceof NoSuchObjectException ||
                 t instanceof UnmarshalException ||
                 t instanceof MarshalException;
    }
}
