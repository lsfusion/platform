package lsfusion.client.controller.remote.proxy;

import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.base.remote.PendingRemoteInterface;
import lsfusion.interop.base.remote.RemoteRequestInterface;

import java.rmi.RemoteException;

public abstract class RemoteRequestObjectProxy<T extends RemoteRequestInterface> extends PendingRemoteObjectProxy<T> implements RemoteRequestInterface {

    public RemoteRequestObjectProxy(T target, String realHostName) {
        super(target, realHostName);
    }

    @Override
    public ServerResponse continueServerInvocation(long requestIndex, long lastReceivedRequestIndex, int continueIndex, Object[] actionResults) throws RemoteException {
        logRemoteMethodStartCall("continueServerInvocation");
        ServerResponse result = target.continueServerInvocation(requestIndex, lastReceivedRequestIndex, continueIndex, actionResults);
        logRemoteMethodEndCall("continueServerInvocation", result);
        return result;
    }

    @Override
    public ServerResponse throwInServerInvocation(long requestIndex, long lastReceivedRequestIndex, int continueIndex, Throwable clientThrowable) throws RemoteException {
        logRemoteMethodStartCall("throwInServerInvocation");
        ServerResponse result = target.throwInServerInvocation(requestIndex, lastReceivedRequestIndex, continueIndex, clientThrowable);
        logRemoteMethodEndCall("throwInServerInvocation", result);
        return result;
    }

    @Override
    public boolean isInServerInvocation(long requestIndex) throws RemoteException {
        logRemoteMethodStartCall("isInServerInvocation");
        boolean result = target.isInServerInvocation(requestIndex);
        logRemoteMethodEndCall("isInServerInvocation", result);
        return result;
    }
}
