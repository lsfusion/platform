package platform.client.remote.proxy;

import platform.interop.remote.MethodInvocation;
import platform.interop.remote.PendingRemote;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class RemoteObjectProxy<T extends PendingRemote> implements PendingRemote {

    private List<MethodInvocation> pendingInvocations = new ArrayList<MethodInvocation>();

    protected T target;

    public RemoteObjectProxy(T target) {
        this.target = target;
    }

    public Object execute(MethodInvocation[] invocations) throws RemoteException {
        return target.execute(invocations);
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
}
