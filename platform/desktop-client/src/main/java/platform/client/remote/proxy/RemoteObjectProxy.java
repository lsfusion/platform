package platform.client.remote.proxy;

import com.google.common.base.Throwables;
import org.apache.log4j.Logger;
import platform.base.EProvider;
import platform.client.form.BusyDisplayer;
import platform.interop.RemoteContextInterface;
import platform.interop.remote.MethodInvocation;
import platform.interop.remote.PendingRemote;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public abstract class RemoteObjectProxy<T extends PendingRemote> implements PendingRemote {
    private static Logger logger = Logger.getLogger(RemoteFormProxy.class);

    private List<MethodInvocation> pendingInvocations = new ArrayList<MethodInvocation>();

    public Map<String, String> blockedScreen;

    protected T target;

    public RemoteObjectProxy(T target) {
        this.target = target;
    }

    @NonFlushRemoteMethod
    public Object execute(final MethodInvocation[] invocations) throws RemoteException {
        logRemoteMethodStartCall("execute");

        if (logger.isDebugEnabled()) {
            for (MethodInvocation invocation : invocations) {
                logger.debug("  Invocation in execute: " + invocation.toString());
            }
        }

        Object result = executeBlocked(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return target.execute(invocations);
            }
        });

        logRemoteMethodEndCall("execute", result);
        return result;
    }

    @NonFlushRemoteMethod
    public Object[] createAndExecute(final MethodInvocation creator, final MethodInvocation[] invocations) throws RemoteException {
        logRemoteMethodStartCall("createAndExecute");

        if (logger.isDebugEnabled()) {
            logger.debug("  Creator   in  createAndExecute: " + creator.toString());
            for (MethodInvocation invocation : invocations) {
                logger.debug("  Invocation in createAndExecute: " + invocation.toString());
            }
        }

        Object[] result = executeBlocked(new Callable<Object[]>() {
            @Override
            public Object[] call() throws Exception {
                return target.createAndExecute(creator, invocations);
            }
        });

        logRemoteMethodEndCall("createAndExecute", result);
        return result;
    }

    @NonFlushRemoteMethod
    private <R> R executeBlocked(Callable<R> remoteExecute) {
        BusyDisplayer busyDisplayer = new BusyDisplayer(new EProvider<String>() {
            @Override
            public String getExceptionally() throws RemoteException {
                return ((RemoteContextInterface) target).getRemoteActionMessage();
            }
        });
        busyDisplayer.start();

        try {
            return remoteExecute.call();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            busyDisplayer.stop();
        }
    }

    @NonFlushRemoteMethod
    public void addPendingInvocation(MethodInvocation invocation) {
        pendingInvocations.add(invocation);
    }

    @NonFlushRemoteMethod
    public Object flushPendingInvocations() throws RemoteException {
        if (pendingInvocations.size() > 0) {
            try {
                return execute(pendingInvocations.toArray(new MethodInvocation[pendingInvocations.size()]));
            } finally {
                pendingInvocations.clear();
            }
        }
        return null;
    }

    Map<Object, Object> properties = new HashMap<Object, Object>();

    @NonFlushRemoteMethod
    public Object getProperty(Object key) {
        return properties.get(key);
    }

    @NonFlushRemoteMethod
    public void setProperty(Object key, Object value) {
        properties.put(key, value);
    }

    @NonFlushRemoteMethod
    public boolean hasProperty(Object key) {
        return properties.containsKey(key);
    }

    private long startCall = 0;

    @NonFlushRemoteMethod
    protected void logRemoteMethodStartCall(String methodName) {
        if (logger.isInfoEnabled()) {
            startCall = System.currentTimeMillis();
            logger.info("Calling remote method: " + this.getClass().getSimpleName() + "." + methodName);
        }
    }

    @NonFlushRemoteMethod
    protected void logRemoteMethodEndCall(String methodName, Object result) {
        if (logger.isInfoEnabled()) {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            try {
                new ObjectOutputStream(outStream).writeObject(result);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            logger.info(
                    String.format("Remote method called (time: %1$d ms.; result size: %2$d): %3$s.%4$s",
                            System.currentTimeMillis() - startCall, outStream.size(), this.getClass().getSimpleName(), methodName));
        }
    }

    @NonFlushRemoteMethod
    protected void logRemoteMethodStartVoidCall(String methodName) {
        logRemoteMethodStartCall(methodName);
    }

    @NonFlushRemoteMethod
    protected void logRemoteMethodEndVoidCall(String methodName) {
        logRemoteMethodEndCall(methodName, null);
    }

    @NonFlushRemoteMethod
    public static List<MethodInvocation> getImmutableMethodInvocations(Class clazz) {
        List<MethodInvocation> invocations = new ArrayList<MethodInvocation>();
        for (Method method : clazz.getMethods()) {
            if (method.getAnnotation(ImmutableMethod.class) == null) {
                continue;
            }

            // естественно, разрешены только функи без параметров
            assert method.getParameterTypes().length == 0;

            MethodInvocation invocation = new MethodInvocation(method.getName(), new Class[0], new Object[0], method.getReturnType());
            invocations.add(invocation);
        }

        return invocations;
    }
}
