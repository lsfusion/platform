package lsfusion.gwt.form.server.logics.spring;

import lsfusion.gwt.base.server.spring.InvalidateListener;
import lsfusion.interop.RemoteLogicsInterface;

import java.rmi.RemoteException;

public interface LogicsProvider<T extends RemoteLogicsInterface> {
    String getRegistryHost();
    int getRegistryPort();
    String getExportName();

    T getLogics() throws RemoteException;

    void invalidate();

    void addInvalidateListener(InvalidateListener listener);
    void removeInvalidateListener(InvalidateListener listener);
}
