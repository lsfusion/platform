package lsfusion.interop.navigator;

import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.PendingRemoteInterface;
import lsfusion.interop.navigator.callback.ClientCallBackInterface;

import java.rmi.RemoteException;

public interface RemoteNavigatorInterface extends PendingRemoteInterface {

    // separate methods, because has complex response serialization (and it will be an overhead using virtual navigator actions anyway)

    byte[] getNavigatorTree() throws RemoteException;

    ClientSettings getClientSettings() throws RemoteException;

    // main interface

    ServerResponse executeNavigatorAction(String actionSID, int type) throws RemoteException;

    ServerResponse continueNavigatorAction(Object[] actionResults) throws RemoteException;

    ServerResponse throwInNavigatorAction(Throwable clientThrowable) throws RemoteException;

    void logClientException(String title, String hostname, Throwable t) throws RemoteException;

    void close() throws RemoteException;

    // separate methods, because used really often (and it will be an overhead using virtual navigator actions anyway)

    void setCurrentForm(String formID) throws RemoteException;
    String getCurrentForm() throws RemoteException;
    ClientCallBackInterface getClientCallBack() throws RemoteException;

}