package lsfusion.interop.navigator.remote;

import lsfusion.base.Pair;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.connection.RemoteConnectionInterface;
import lsfusion.interop.form.remote.RemoteFormInterface;
import lsfusion.interop.navigator.ClientInfo;

import java.rmi.RemoteException;

public interface RemoteNavigatorInterface extends RemoteConnectionInterface {

    // separate methods, because has complex response serialization (and it will be an overhead using virtual navigator actions anyway)

    byte[] getNavigatorTree() throws RemoteException;

    // main interface

    void voidNavigatorAction(long requestIndex, long lastReceivedRequestIndex, long waitRequestIndex) throws RemoteException;

    ServerResponse executeNavigatorAction(long requestIndex, long lastReceivedRequestIndex, String script) throws RemoteException;

    ServerResponse executeNavigatorAction(long requestIndex, long lastReceivedRequestIndex, String actionSID, int type) throws RemoteException;

    void logClientException(String hostname, Throwable t) throws RemoteException;

    // separate methods, because used really often (and it will be an overhead using virtual navigator actions anyway)

    ClientCallBackInterface getClientCallBack() throws RemoteException;

    // external
    
    Pair<RemoteFormInterface, String> createFormExternal(String json) throws RemoteException;

    void updateClientInfo(ClientInfo clientInfo) throws RemoteException;

    void saveSubscription(String subscription) throws RemoteException;
}