package lsfusion.interop.base.remote;

import lsfusion.interop.action.ServerResponse;

import java.rmi.RemoteException;

public interface RemoteRequestInterface extends PendingRemoteInterface {

    ServerResponse continueServerInvocation(long requestIndex, long lastReceivedRequestIndex, int continueIndex, Object[] actionResults) throws RemoteException;

    ServerResponse throwInServerInvocation(long requestIndex, long lastReceivedRequestIndex, int continueIndex, Throwable clientThrowable) throws RemoteException;

    boolean isInServerInvocation(long requestIndex) throws RemoteException;
}
