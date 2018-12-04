package lsfusion.gwt.form.server.logics.spring;

import lsfusion.gwt.base.server.spring.InvalidateListener;
import lsfusion.gwt.form.server.navigator.spring.NavigatorProvider;
import lsfusion.gwt.form.shared.view.GNavigator;
import lsfusion.interop.RemoteLogicsInterface;

import java.io.IOException;
import java.rmi.RemoteException;

public interface LogicsProvider<T extends RemoteLogicsInterface> {

    GNavigator createNavigator(String logicsID, LogicsSessionObject logicsSessionObject, NavigatorProvider navigatorProvider) throws IOException;
    LogicsSessionObject getLogicsSessionObject()

    String getRegistryHost();
    int getRegistryPort();
    String getExportName();

    T getLogics() throws RemoteException;

    void invalidate();

    void addInvalidateListener(InvalidateListener listener);
    void removeInvalidateListener(InvalidateListener listener);
}
