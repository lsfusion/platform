package platform.client.remote.proxy;

import org.apache.log4j.Logger;
import platform.client.Main;
import platform.client.SwingUtils;
import platform.client.form.BlockingTask;
import platform.interop.remote.MethodInvocation;
import platform.interop.remote.PendingRemote;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.*;
import java.util.List;

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

        boolean screenBlock = false;
        if (logger.isDebugEnabled()) {
            for (MethodInvocation invocation : invocations) {
                logger.debug("  Invocation in execute: " + invocation.toString());
            }
        }

        for (MethodInvocation invocation : invocations) {
            screenBlock |= (blockedScreen != null) && (blockedScreen.containsKey(invocation.name) && invocation.args.length > 0 && invocation.args[0].toString().equals(blockedScreen.get(invocation.name)));
        }

        Window window = SwingUtils.getActiveWindow();
        if (window != null) {
            window.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }

        TimerTask task = new BlockingTask();
        Timer timer = new Timer();
        timer.schedule(task, screenBlock ? 0 : 2500);
        Object result = target.execute(invocations);
        timer.cancel();

        if (window != null) {
            window.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            window.repaint();
        }

        logRemoteMethodEndCall("execute", result);
        return result;
    }

    @NonFlushRemoteMethod
    public Object[] createAndExecute(final MethodInvocation creator, final MethodInvocation[] invocations) throws RemoteException {
        logRemoteMethodStartCall("createAndExecute");

        boolean screenBlock = false;
        if (logger.isDebugEnabled()) {
            logger.debug("  Creator   in  createAndExecute: " + creator.toString());
            for (MethodInvocation invocation : invocations) {
                logger.debug("  Invocation in createAndExecute: " + invocation.toString());
            }
        }

        for (MethodInvocation invocation : invocations) {
            screenBlock |= (blockedScreen != null) && (blockedScreen.containsKey(invocation.name) && invocation.args.length > 0 && invocation.args[0].toString().equals(blockedScreen.get(invocation.name)));
        }

        Window window = SwingUtils.getActiveWindow();
        if (window != null) {
            window.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }

        TimerTask task = new BlockingTask();
        Timer timer = new Timer();
        timer.schedule(task, screenBlock ? 0 : 2500);
        Object[] result = target.createAndExecute(creator, invocations);
        timer.cancel();

        if (window != null) {
            window.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            window.repaint();
        }
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
