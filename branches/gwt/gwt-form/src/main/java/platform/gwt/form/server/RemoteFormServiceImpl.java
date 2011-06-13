package platform.gwt.form.server;

import net.customware.gwt.dispatch.server.InstanceActionHandlerRegistry;
import platform.gwt.base.server.LogicsDispatchServlet;
import platform.gwt.form.server.handlers.ChangeGroupObjectHandler;
import platform.gwt.form.server.handlers.GetFormHandler;
import platform.gwt.form.server.handlers.GetRemoteChangesHandler;
import platform.gwt.form.server.handlers.SetRegularFilterHandler;

public class RemoteFormServiceImpl extends LogicsDispatchServlet {
    @Override
    protected void addHandlers(InstanceActionHandlerRegistry registry) {
        registry.addHandler(new GetFormHandler(this));
        registry.addHandler(new ChangeGroupObjectHandler(this));
        registry.addHandler(new GetRemoteChangesHandler(this));
        registry.addHandler(new SetRegularFilterHandler(this));
    }
}
