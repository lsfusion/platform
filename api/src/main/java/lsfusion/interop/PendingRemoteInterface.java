package lsfusion.interop;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface PendingRemoteInterface extends Remote {
    Object[] createAndExecute(PendingMethodInvocation creator, PendingMethodInvocation[] invocations) throws RemoteException;
    String getRemoteActionMessage() throws RemoteException;
    List<Object> getRemoteActionMessageList() throws RemoteException;
    void interrupt(boolean cancelable) throws RemoteException;
}
