package platform.gwt.base.server.spring;

import platform.interop.RemoteLogicsInterface;

public interface BusinessLogicsProvider<T extends RemoteLogicsInterface> {
    T getLogics();

    void invalidate();
}
