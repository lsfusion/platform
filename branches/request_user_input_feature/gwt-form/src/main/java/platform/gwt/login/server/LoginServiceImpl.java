package platform.gwt.login.server;

import net.customware.gwt.dispatch.server.InstanceActionHandlerRegistry;
import platform.gwt.base.server.LogicsDispatchServlet;
import platform.gwt.login.server.handlers.AddUserHandler;
import platform.gwt.login.server.handlers.RemindPasswordHandler;
import platform.interop.RemoteLogicsInterface;

public class LoginServiceImpl extends LogicsDispatchServlet<RemoteLogicsInterface> {
    @Override
    protected void addHandlers(InstanceActionHandlerRegistry registry) {
        registry.addHandler(new RemindPasswordHandler(this));
        registry.addHandler(new AddUserHandler(this));
    }
}