package lsfusion.client.controller.remote.proxy;

import com.google.common.base.Throwables;
import lsfusion.base.remote.ZipClientSocketFactory;
import lsfusion.client.base.log.ClientLoggers;
import lsfusion.client.base.utils.ContentLengthException;
import lsfusion.client.base.utils.ContentLengthOutputStream;
import lsfusion.interop.base.remote.PendingRemoteInterface;
import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

// for now it's necessary to proxy only remote objects that can create other remote objects (for example ClientCallbackInterface don't do it)
public abstract class RemoteObjectProxy<T extends Remote> implements Remote {
    private static Logger logger = ClientLoggers.remoteLogger;

    protected final T target;

    private final Map<Object, Object> properties = new HashMap<>();

    private long startCall = 0;

    public final String realHostName;

    public static String getRealHostName(PendingRemoteInterface remote) {
        return ((RemoteObjectProxy)remote).realHostName;
    }

    public RemoteObjectProxy(T target, String realHostName) {
        this.target = target;
        this.realHostName = realHostName;
        assert realHostName != null;
    }

    public Object getProperty(Object key) {
        return properties.get(key);
    }

    public void setProperty(Object key, Object value) {
        properties.put(key, value);
    }

    public boolean hasProperty(Object key) {
        return properties.containsKey(key);
    }

    protected <T> T callImmutableMethod(String methodName, Callable<T> callable) throws Exception {
        logger.debug("Running immutable method: " + methodName);
        if (hasProperty(methodName)) {
            Object result = getProperty(methodName);
            logger.debug("  Returning cached value: " + result);
            return (T) result;
        }
        logger.debug("  Directly call immutable method:");
        return callable.call();
    }

    protected void logRemoteMethodStartCall(String methodName) {
        if (logger.isInfoEnabled()) {
            startCall = System.currentTimeMillis();
            logger.info("Calling remote method: " + this.getClass().getSimpleName() + "." + methodName);
        }
    }

    protected void logRemoteMethodEndCall(String methodName, Object result) {
        if (logger.isInfoEnabled()) {
            try (ContentLengthOutputStream outStream = new ContentLengthOutputStream(new ByteArrayOutputStream(), 128)) {
                try {
                    new ObjectOutputStream(outStream).writeObject(result);
                } catch (ContentLengthException ignored) {
                    //suppress
                }
                logger.info(
                        String.format("Remote method called (time: %1$d ms.; result size: %2$s): %3$s.%4$s",
                                System.currentTimeMillis() - startCall, outStream.size(), this.getClass().getSimpleName(), methodName));
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }
    }

    protected void logRemoteMethodStartVoidCall(String methodName) {
        logRemoteMethodStartCall(methodName);
    }

    protected void logRemoteMethodEndVoidCall(String methodName) {
        logRemoteMethodEndCall(methodName, null);
    }

    @Aspect
    public static class RealHostNameAspect {
        @Around("execution(public * (java.rmi.Remote+ && *..*Interface).*(..))" +
                " && !execution(public * *.ping(..))" +
                " && !execution(public * *.findClass(..))" +
                " && !execution(public * *.toString())" +
                " && target(target)")
        public Object executeRemoteMethod(ProceedingJoinPoint thisJoinPoint, RemoteObjectProxy target) throws Throwable {
            ZipClientSocketFactory.threadRealHostName.set(target.realHostName);
            try {
                return thisJoinPoint.proceed();
            } finally {
                ZipClientSocketFactory.threadRealHostName.set(null);
            }
        }
    }
}
