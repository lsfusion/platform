package lsfusion.client.navigator.controller.remote.proxy;

import com.google.common.base.Throwables;
import lsfusion.base.Pair;
import lsfusion.client.controller.remote.proxy.PendingRemoteObjectProxy;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.remote.RemoteFormInterface;
import lsfusion.interop.navigator.ClientSettings;
import lsfusion.interop.navigator.remote.ClientCallBackInterface;
import lsfusion.interop.navigator.remote.RemoteNavigatorInterface;

import java.rmi.RemoteException;
import java.util.concurrent.Callable;

public class RemoteNavigatorProxy<T extends RemoteNavigatorInterface> extends PendingRemoteObjectProxy<T> implements RemoteNavigatorInterface {

    public RemoteNavigatorProxy(T target, String realHostName) {
        super(target, realHostName);
    }

    public void logClientException(String hostname, Throwable t) throws RemoteException {
        target.logClientException(hostname, t);
    }

    public void close() throws RemoteException {
        logRemoteMethodStartCall("close");
        target.close();
        logRemoteMethodEndCall("close", "");
    }

    public ClientCallBackInterface getClientCallBack() throws RemoteException {
        return target.getClientCallBack();
    }

    @Override
    public Pair<RemoteFormInterface, String> createFormExternal(String json) throws RemoteException {
        return target.createFormExternal(json);
    }

    @Override
    public void setCurrentForm(String formID) throws RemoteException {
        target.setCurrentForm(formID);
    }

    @Override
    public String getCurrentForm() throws RemoteException {
        logRemoteMethodStartCall("getCurrentForm");
        String result = target.getCurrentForm();
        logRemoteMethodEndCall("getCurrentForm", result);
        return result;
    }

    public byte[] getNavigatorTree() throws RemoteException {
        try {
            return callImmutableMethod("getNavigatorTree", new Callable<byte[]>() {
                @Override
                public byte[] call() throws Exception {
                    return target.getNavigatorTree();
                }
            });
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public ClientSettings getClientSettings() throws RemoteException {
        return target.getClientSettings();
    }

    @Override
    public ServerResponse executeNavigatorAction(long requestIndex, long lastReceivedRequestIndex, String script) throws RemoteException {
        return target.executeNavigatorAction(requestIndex, lastReceivedRequestIndex, script);
    }

    @Override
    public ServerResponse executeNavigatorAction(long requestIndex, long lastReceivedRequestIndex, String navigatorActionSID, int type) throws RemoteException {
        return target.executeNavigatorAction(requestIndex, lastReceivedRequestIndex, navigatorActionSID, type);
    }

    @Override
    public ServerResponse continueNavigatorAction(long requestIndex, long lastReceivedRequestIndex, int continueIndex, Object[] actionResults) throws RemoteException {
        return target.continueNavigatorAction(requestIndex, lastReceivedRequestIndex, continueIndex, actionResults);
    }

    @Override
    public ServerResponse throwInNavigatorAction(long requestIndex, long lastReceivedRequestIndex, int continueIndex, Throwable clientThrowable) throws RemoteException {
        return target.throwInNavigatorAction(requestIndex, lastReceivedRequestIndex, continueIndex, clientThrowable);
    }
}
