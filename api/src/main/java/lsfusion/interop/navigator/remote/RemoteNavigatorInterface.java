package lsfusion.interop.navigator.remote;

import lsfusion.base.Pair;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.base.remote.PendingRemoteInterface;
import lsfusion.interop.base.remote.RemoteRequestInterface;
import lsfusion.interop.form.remote.RemoteFormInterface;
import lsfusion.interop.navigator.ClientSettings;

import java.rmi.RemoteException;

public interface RemoteNavigatorInterface extends RemoteRequestInterface {

    // separate methods, because has complex response serialization (and it will be an overhead using virtual navigator actions anyway)

    byte[] getNavigatorTree() throws RemoteException;

    ClientSettings getClientSettings() throws RemoteException;

    // main interface

    ServerResponse executeNavigatorAction(long requestIndex, long lastReceivedRequestIndex, String script) throws RemoteException;

    ServerResponse executeNavigatorAction(long requestIndex, long lastReceivedRequestIndex, String actionSID, int type) throws RemoteException;

    void logClientException(String hostname, Throwable t) throws RemoteException;

    void close() throws RemoteException;

    // separate methods, because used really often (and it will be an overhead using virtual navigator actions anyway)

    void setCurrentForm(String formID) throws RemoteException;
    String getCurrentForm() throws RemoteException;
    ClientCallBackInterface getClientCallBack() throws RemoteException;

    // external
    
    Pair<RemoteFormInterface, String> createFormExternal(String json) throws RemoteException;
}