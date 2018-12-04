package lsfusion.gwt.form.server.navigator.spring;

import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.interop.remote.ClientCallBackInterface;

import java.rmi.RemoteException;

public interface NavigatorProvider {

    NavigatorSessionObject getNavigatorSessionObject(String host, int port) throws RemoteException;

    RemoteNavigatorInterface getNavigator() throws RemoteException;
    void invalidate();
    
    void tabOpened(String tabSID);
    boolean tabClosed(String tabSID);
    String getSessionInfo();
    ClientCallBackInterface getClientCallBack() throws RemoteException; // caching
}
