package lsfusion.client.navigator.controller.remote.proxy;

import lsfusion.base.Pair;
import lsfusion.client.connection.RemoteConnectionProxy;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.remote.RemoteFormInterface;
import lsfusion.interop.navigator.remote.ClientCallBackInterface;
import lsfusion.interop.navigator.remote.RemoteNavigatorInterface;

import java.rmi.RemoteException;

public class RemoteNavigatorProxy<T extends RemoteNavigatorInterface> extends RemoteConnectionProxy<T> implements RemoteNavigatorInterface {

    public RemoteNavigatorProxy(T target, String realHostName) {
        super(target, realHostName);
    }

    public void logClientException(String hostname, Throwable t) throws RemoteException {
        target.logClientException(hostname, t);
    }

    public ClientCallBackInterface getClientCallBack() throws RemoteException {
        return target.getClientCallBack();
    }

    @Override
    public Pair<RemoteFormInterface, String> createFormExternal(String json) throws RemoteException {
        return target.createFormExternal(json);
    }

    public byte[] getNavigatorTree() throws RemoteException {
        return target.getNavigatorTree();
    }

    @Override
    public void voidNavigatorAction(long requestIndex, long lastReceivedRequestIndex, long waitRequestIndex) throws RemoteException {
        target.voidNavigatorAction(requestIndex, lastReceivedRequestIndex, waitRequestIndex);
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
    public void updateNavigatorClientSettings(String screenSize, boolean mobile) throws RemoteException {
        target.updateNavigatorClientSettings(screenSize, mobile);
    }
}
