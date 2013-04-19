package platform.gwt.base.server.spring;

import platform.interop.RemoteLogicsInterface;

public interface BusinessLogicsProvider<T extends RemoteLogicsInterface> {
    String getRegistryHost();
    int getRegistryPort();
    String getExportName();

    T getLogics();

    void invalidate();

    void addInvalidateListener(InvalidateListener listener);
    void removeInvalidateListener(InvalidateListener listener);
}
