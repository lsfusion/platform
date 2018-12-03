package lsfusion.gwt.base.server.spring;

import lsfusion.interop.RemoteLogicsInterface;

import java.rmi.RemoteException;

public interface BusinessLogicsProvider<T extends RemoteLogicsInterface> {
    String getRegistryHost();
    int getRegistryPort();
    String getExportName();

    T getLogics() throws RemoteException;

    void invalidate();

    void addInvalidateListener(InvalidateListener listener);
    void removeInvalidateListener(InvalidateListener listener);
}
