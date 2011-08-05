package platform.gwt.navigator.server;

import net.customware.gwt.dispatch.server.InstanceActionHandlerRegistry;
import platform.gwt.base.server.LogicsDispatchServlet;
import platform.gwt.navigator.server.handlers.GetNavigatorElementsHandler;
import platform.interop.RemoteLogicsInterface;

public class NavigatorServiceImpl extends LogicsDispatchServlet<RemoteLogicsInterface> {
    @Override
    protected void addHandlers(InstanceActionHandlerRegistry registry) {
        registry.addHandler(new GetNavigatorElementsHandler(this));
    }
}
