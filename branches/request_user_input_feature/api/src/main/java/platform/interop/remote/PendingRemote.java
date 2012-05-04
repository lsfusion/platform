package platform.interop.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PendingRemote extends Remote {

    public Object execute(MethodInvocation[] invocations) throws RemoteException;
    public Object[] createAndExecute(MethodInvocation creator, MethodInvocation[] invocations) throws RemoteException;
}
