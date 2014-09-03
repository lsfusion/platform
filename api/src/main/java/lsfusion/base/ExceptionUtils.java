package lsfusion.base;

import com.google.common.base.Throwables;
import lsfusion.interop.exceptions.RemoteServerException;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.rmi.*;

public class ExceptionUtils {
    public static Throwable getRootCause(Throwable throwable) {
        Throwable result = throwable;
        while (result != null && result.getCause() != null) {
            result = result.getCause();
        }

        return result;
    }

    public static String getStackTrace() {
        return getStackTrace(new Exception());
    }

    public static String getStackTrace(Throwable e) {
        return getStackTrace(e.getStackTrace());
    }
    
    public static String getStackTrace(StackTraceElement[] trace) {
        String s = "";
        for (StackTraceElement aTrace : trace)
            s += "\tat " + aTrace + '\n';
        return s;
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
        try {
            t.printStackTrace(new PrintStream(stackStream, false, "UTF-8"));
            return stackStream.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw Throwables.propagate(e);
        }
    }

    public static int getFatalRemoteExceptionCount(Throwable t) {
        t = getRootCause(t);
        
        // скорее всего уже не восстановимся сервел уничтожил объект на сервере
        if(t instanceof NoSuchObjectException)
            return 3;
        
        // временная проблема со связью
        if(t instanceof ConnectException || t instanceof java.net.ConnectException || t instanceof SocketException || t instanceof UnknownHostException || t instanceof java.net.UnknownHostException) // проблема со связью ждем бесконечно
            return Integer.MAX_VALUE;
        
        return 10; // неизвестно что
    }
}
