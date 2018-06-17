package lsfusion.gwt.base.server.spring;

import lsfusion.interop.navigator.RemoteNavigatorInterface;

import java.rmi.RemoteException;

public interface NavigatorProvider {
    RemoteNavigatorInterface getNavigator() throws RemoteException;
    void invalidate();
}
