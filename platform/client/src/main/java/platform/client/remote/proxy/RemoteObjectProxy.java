package platform.client.remote.proxy;

import platform.interop.remote.MethodInvocation;
import platform.interop.remote.PendingRemote;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.*;

public abstract class RemoteObjectProxy<T extends PendingRemote> implements PendingRemote {

    private List<MethodInvocation> pendingInvocations = new ArrayList<MethodInvocation>();

    protected T target;

    public RemoteObjectProxy(T target) {
        this.target = target;
    }

    public Object execute(MethodInvocation[] invocations) throws RemoteException {
        return target.execute(invocations);
    }

    public Object[] createAndExecute(MethodInvocation creator, MethodInvocation[] invocations) throws RemoteException {
        return target.createAndExecute(creator, invocations);
    }

    @NonFlushRemoteMethod
    public void addPendingInvocation(MethodInvocation invocation) {
        pendingInvocations.add(invocation);
    }

    @NonFlushRemoteMethod
    public Object flushPendingInvocations() throws RemoteException {
        Object ret = target.execute(pendingInvocations.toArray(new MethodInvocation[pendingInvocations.size()]));

        pendingInvocations.clear();

        return ret;
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
