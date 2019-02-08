package lsfusion.interop.navigator;

import lsfusion.interop.ClientSettings;
import lsfusion.interop.form.RemoteFormInterface;
import lsfusion.interop.form.ServerResponse;
import lsfusion.interop.remote.ClientCallBackInterface;
import lsfusion.interop.remote.PendingRemoteInterface;

import java.rmi.RemoteException;
import java.util.Map;

public interface RemoteNavigatorInterface extends PendingRemoteInterface {

    byte[] getNavigatorTree() throws RemoteException;

    // окна лог, релевантные классы, статус и т.п.
    byte[] getCommonWindows() throws RemoteException;

    ServerResponse executeNavigatorAction(String actionSID, int type) throws RemoteException;

    ServerResponse continueNavigatorAction(Object[] actionResults) throws RemoteException;

    ServerResponse throwInNavigatorAction(Throwable clientThrowable) throws RemoteException;

    RemoteFormInterface createForm(String formSID, Map<String, String> initialObjects, boolean isModal, boolean interactive) throws RemoteException;

    void logClientException(String title, String hostname, Throwable t) throws RemoteException;

    void close() throws RemoteException;

    //for notifications
    void setCurrentForm(String formID) throws RemoteException;
    String getCurrentForm() throws RemoteException;

    // пингование сервера
    ClientCallBackInterface getClientCallBack() throws RemoteException;

    ClientSettings getClientSettings() throws RemoteException;
}