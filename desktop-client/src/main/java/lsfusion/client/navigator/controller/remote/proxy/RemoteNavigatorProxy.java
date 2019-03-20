package lsfusion.client.navigator.controller.remote.proxy;

import com.google.common.base.Throwables;
import lsfusion.client.controller.remote.proxy.RemoteObjectProxy;
import lsfusion.interop.navigator.ClientSettings;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.interop.navigator.callback.ClientCallBackInterface;

import java.rmi.RemoteException;
import java.util.concurrent.Callable;

public class RemoteNavigatorProxy<T extends RemoteNavigatorInterface> extends RemoteObjectProxy<T> implements RemoteNavigatorInterface {

    public RemoteNavigatorProxy(T target) {
        super(target);
    }

    public void logClientException(String title, String hostname, Throwable t) throws RemoteException {
        target.logClientException(title, hostname, t);
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
    public ServerResponse executeNavigatorAction(String script) throws RemoteException {
        return target.executeNavigatorAction(script);
    }

    @Override
    public ServerResponse executeNavigatorAction(String navigatorActionSID, int type) throws RemoteException {
        return target.executeNavigatorAction(navigatorActionSID, type);
    }

    @Override
    public ServerResponse continueNavigatorAction(Object[] actionResults) throws RemoteException {
        return target.continueNavigatorAction(actionResults);
    }

    @Override
    public ServerResponse throwInNavigatorAction(Throwable clientThrowable) throws RemoteException {
        return target.throwInNavigatorAction(clientThrowable);
    }
}
