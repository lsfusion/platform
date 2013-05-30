package platform.gwt.login.server;

import net.customware.gwt.dispatch.server.InstanceActionHandlerRegistry;
import platform.gwt.base.server.LogicsAwareDispatchServlet;
import platform.gwt.login.server.handlers.AddUserHandler;
import platform.gwt.login.server.handlers.RemindPasswordHandler;
import platform.interop.RemoteLogicsInterface;

public class LoginDispatchServlet extends LogicsAwareDispatchServlet<RemoteLogicsInterface> {
    @Override
    protected void addHandlers(InstanceActionHandlerRegistry registry) {
        registry.addHandler(new RemindPasswordHandler(this));
        registry.addHandler(new AddUserHandler(this));
    }
}