package lsfusion.client.controller.remote.proxy;

import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.base.remote.RemoteRequestInterface;

import java.rmi.RemoteException;

public abstract class RemoteRequestObjectProxy<T extends RemoteRequestInterface> extends PendingRemoteObjectProxy<T> implements RemoteRequestInterface {

    public RemoteRequestObjectProxy(T target, String realHostName) {
        super(target, realHostName);
    }

    @Override
    public ServerResponse continueServerInvocation(long requestIndex, long lastReceivedRequestIndex, int continueIndex, Object actionResult) throws RemoteException {
        logRemoteMethodStartCall("continueServerInvocation");
        ServerResponse result = target.continueServerInvocation(requestIndex, lastReceivedRequestIndex, continueIndex, actionResult);
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

    @Override
    public ServerResponse exec(long requestIndex, long lastReceivedRequestIndex, String action, Object[] params) throws RemoteException {
        logRemoteMethodStartVoidCall("exec");
        ServerResponse result = target.exec(requestIndex, lastReceivedRequestIndex, action, params);
        logRemoteMethodEndVoidCall("exec");
        return result;
    }

    @Override
    public ServerResponse eval(long requestIndex, long lastReceivedRequestIndex, String script, boolean evalAction, Object[] params) throws RemoteException {
        logRemoteMethodStartVoidCall("eval");
        ServerResponse result = target.eval(requestIndex, lastReceivedRequestIndex, script, evalAction, params);
        logRemoteMethodEndVoidCall("eval");
        return result;
    }

    @Override
    public ServerResponse change(long requestIndex, long lastReceivedRequestIndex, String property, Object[] params, Object value) throws RemoteException {
        logRemoteMethodStartVoidCall("change");
        ServerResponse result = target.change(requestIndex, lastReceivedRequestIndex, property, params, value);
        logRemoteMethodEndVoidCall("change");
        return result;
    }
}
