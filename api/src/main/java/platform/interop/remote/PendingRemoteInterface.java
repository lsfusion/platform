package platform.interop.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PendingRemoteInterface extends Remote {
    public Object[] createAndExecute(MethodInvocation creator, MethodInvocation[] invocations) throws RemoteException;
    String getRemoteActionMessage() throws RemoteException;
}
