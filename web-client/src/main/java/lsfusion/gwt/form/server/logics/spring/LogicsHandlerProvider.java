package lsfusion.gwt.form.server.logics.spring;

import lsfusion.gwt.base.server.spring.InvalidateListener;
import lsfusion.gwt.form.server.navigator.spring.LogicsAndNavigatorProvider;
import lsfusion.gwt.form.shared.view.GNavigator;
import lsfusion.interop.RemoteLogicsInterface;

import java.io.IOException;
import java.rmi.RemoteException;

public interface LogicsHandlerProvider {

    String getRegistryHost();
    int getRegistryPort();
    String getExportName();

    RemoteLogicsInterface getLogics() throws RemoteException;
    RemoteLogicsInterface getLogics(String host, Integer port, String exportName) throws RemoteException;
    void invalidate();
}
