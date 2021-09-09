package lsfusion.base;

import com.google.common.base.Throwables;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.rmi.ConnectException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.UnknownHostException;
import java.util.concurrent.TimeoutException;

public class ExceptionUtils {
    public static Throwable getRootCause(Throwable throwable) {
        Throwable result = throwable;
        while (result != null && result.getCause() != null) {
            result = result.getCause();
        }

        return result;
    }

    public static String getPlainStackTrace() {
        return getStackTrace().replace('\n', '\t');
    }

    public static String getStackTrace() {
        return getStackTrace(new Exception());
    }

    public static String toString(Throwable e) {
        e = ExceptionUtils.getRootCause(e);
        return e.getMessage() + '\n' + getStackTrace(e);
    }

    public static String getStackTrace(Throwable e) {
        assert e.getCause() == null;
        return getStackTrace(e.getStackTrace());
    }
    
    public static String getStackTrace(Thread thread) {
        return getStackTrace(thread.getStackTrace());        
    }
    
    public static String getStackTrace(StackTraceElement[] trace) {
        String s = "";
        for (StackTraceElement aTrace : trace)
            s += "\tat " + aTrace + '\n';
        return s;
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
        if (t instanceof NoSuchObjectException) {
            return 3;
        } 
        
        // временная проблема со связью
        if(t instanceof ConnectException 
                || t instanceof java.net.ConnectException 
                || t instanceof SocketException 
                || t instanceof UnknownHostException 
                || t instanceof java.net.UnknownHostException 
                || t instanceof TimeoutException) // проблема со связью ждем бесконечно
            return 20;
        
        return 10; // неизвестно что
    }

    public static String getExStackTrace(String javaStack, String lsfStack) {
        return lsfStack + '\n' + javaStack;
    }

    public static RuntimeException propagateWithMessage(Throwable throwable, String message) {
        RuntimeException propagatedMessage = new RuntimeException(copyMessage(throwable) + ' ' + message);
        copyStackTraces(throwable, propagatedMessage);
        throw propagatedMessage; 
    }

    // the same as in GExceptionManager
    // when class of throwable changes
    public static String copyMessage(Throwable throwable) {
        throwable = getRootCause(throwable); // also it may make sense to show also messages of chained exceptions, but for now will show only root
        return throwable.getClass().getName() + " " + throwable.getMessage();
    }

    // the same as in GExceptionManager
    // assuming that here should be primitive copy (Strings and other very primitive Java classes) to be deserialized everywhere
    public static void copyStackTraces(Throwable from, Throwable to) {
        from = getRootCause(from); // chained exception stacks are pretty useless (they are always the same as root + line in catch, which is usually pretty evident)
        to.setStackTrace(from.getStackTrace());
    }
}
