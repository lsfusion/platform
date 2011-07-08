package platform.gwt.base.server.spring;

import platform.interop.RemoteLogicsInterface;
import platform.interop.navigator.RemoteNavigatorInterface;

public interface NavigatorProvider<T extends RemoteLogicsInterface> {
    RemoteNavigatorInterface getNavigator();
    void setBusinessLogicsProvider(BusinessLogicsProvider<T> businesslogicsProvider);
}
