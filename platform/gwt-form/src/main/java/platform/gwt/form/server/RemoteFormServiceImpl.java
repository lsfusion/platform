package platform.gwt.form.server;

import net.customware.gwt.dispatch.server.InstanceActionHandlerRegistry;
import platform.gwt.base.server.LogicsDispatchServlet;
import platform.gwt.form.server.handlers.*;
import platform.interop.RemoteLogicsInterface;

public class RemoteFormServiceImpl extends LogicsDispatchServlet<RemoteLogicsInterface> {
    @Override
    protected void addHandlers(InstanceActionHandlerRegistry registry) {
        registry.addHandler(new ChangeClassViewHandler(this));
        registry.addHandler(new GetFormHandler(this));
        registry.addHandler(new ChangeGroupObjectHandler(this));
        registry.addHandler(new GetRemoteChangesHandler(this));
        registry.addHandler(new SetRegularFilterHandler(this));
        registry.addHandler(new ApplyChangesHandler(this));
        registry.addHandler(new CancelChangesHandler(this));
        registry.addHandler(new ChangePropertyHandler(this));
        registry.addHandler(new CreateEditorFormHandler(this));
    }
}
