package lsfusion.gwt.login.server;

import net.customware.gwt.dispatch.server.InstanceActionHandlerRegistry;
import lsfusion.gwt.base.server.LogicsAwareDispatchServlet;
import lsfusion.gwt.login.server.handlers.AddUserHandler;
import lsfusion.gwt.login.server.handlers.RemindPasswordHandler;
import lsfusion.interop.RemoteLogicsInterface;

public class LoginDispatchServlet extends LogicsAwareDispatchServlet<RemoteLogicsInterface> {
    @Override
    protected void addHandlers(InstanceActionHandlerRegistry registry) {
        registry.addHandler(new RemindPasswordHandler(this));
        registry.addHandler(new AddUserHandler(this));
    }
}