package platform.client.remote.proxy;

import platform.interop.remote.MethodInvocation;
import platform.interop.remote.PendingRemote;
import platform.interop.CompressingOutputStream;

import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;

public abstract class RemoteObjectProxy<T extends PendingRemote> implements PendingRemote {
    private static Logger logger = Logger.getLogger(RemoteFormProxy.class.getName());

    private List<MethodInvocation> pendingInvocations = new ArrayList<MethodInvocation>();

    protected T target;

    public RemoteObjectProxy(T target) {
        this.target = target;
    }

    @NonFlushRemoteMethod
    public Object execute(MethodInvocation[] invocations) throws RemoteException {
        logRemoteMethodStartCall("execute");

        if (logger.isLoggable(Level.FINEST)) {
            for (MethodInvocation invocation : invocations) {
                logger.finest("  Invocation in execute: " + invocation.toString());
            }
        }

        Object result = target.execute(invocations);
        logRemoteMethodEndCall("execute", result);
        return result;
    }

    @NonFlushRemoteMethod
    public Object[] createAndExecute(MethodInvocation creator, MethodInvocation[] invocations) throws RemoteException {
        logRemoteMethodStartCall("createAndExecute");
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest("  Creator   in  createAndExecute: " + creator.toString());
            for (MethodInvocation invocation : invocations) {
                logger.finest("  Invocation in createAndExecute: " + invocation.toString());
            }
        }

        Object[] result = target.createAndExecute(creator, invocations);
        logRemoteMethodEndCall("createAndExecute", result);
        return result;
    }

    @NonFlushRemoteMethod
    public void addPendingInvocation(MethodInvocation invocation) {
        pendingInvocations.add(invocation);
    }

    @NonFlushRemoteMethod
    public Object flushPendingInvocations() throws RemoteException {
        if (pendingInvocations.size() > 0) {
            Object ret = execute(pendingInvocations.toArray(new MethodInvocation[pendingInvocations.size()]));

            pendingInvocations.clear();

            return ret;
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
        if (logger.isLoggable(Level.FINE)) {
            startCall = System.currentTimeMillis(); 
            logger.fine("Calling remote method: " + this.getClass().getSimpleName() + "." + methodName);
        }
    }

    @NonFlushRemoteMethod
    protected void logRemoteMethodEndCall(String methodName, Object result) {
        if (logger.isLoggable(Level.FINE)) {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            try {
                new ObjectOutputStream(outStream).writeObject(result);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            logger.fine("Remote method called: " + (System.currentTimeMillis()-startCall) + " " + outStream.size() + " " + this.getClass().getSimpleName() + "." + methodName);
        }
    }

    @NonFlushRemoteMethod
    protected void logRemoteMethodStartVoidCall(String methodName) {
        logRemoteMethodStartCall(methodName);
    }
    @NonFlushRemoteMethod
    protected void logRemoteMethodEndVoidCall(String methodName) {
        logRemoteMethodEndCall(methodName,null);
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
