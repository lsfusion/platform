package lsfusion.gwt.form.server;

import lsfusion.gwt.base.server.LogicsAwareDispatchServlet;
import lsfusion.gwt.form.server.form.handlers.*;
import lsfusion.gwt.form.server.navigator.handlers.*;
import lsfusion.interop.RemoteLogicsInterface;
import net.customware.gwt.dispatch.server.InstanceActionHandlerRegistry;
import org.springframework.beans.factory.annotation.Autowired;

public class FormDispatchServlet extends LogicsAwareDispatchServlet<RemoteLogicsInterface> {
    @Autowired
    private FormSessionManager formSessionManager;

    @Override
    protected void addHandlers(InstanceActionHandlerRegistry registry) {
        registry.addHandler(new GetNavigatorInfoHandler(this));
        registry.addHandler(new GenerateIDHandler(this));
        registry.addHandler(new ShowDefaultFormsHandler(this));

        registry.addHandler(new ChangeClassViewHandler(this));
        registry.addHandler(new ChangePropertyOrderHandler(this));
        registry.addHandler(new ClearPropertyOrdersHandler(this));
        registry.addHandler(new ExpandGroupObjectHandler(this));
        registry.addHandler(new CollapseGroupObjectHandler(this));
        registry.addHandler(new GetFormHandler(this));
        registry.addHandler(new ChangeGroupObjectHandler(this));
        registry.addHandler(new ScrollToEndHandler(this));
        registry.addHandler(new GetRemoteChangesHandler(this));
        registry.addHandler(new SetRegularFilterHandler(this));
        registry.addHandler(new SetTabVisibleHandler(this));
        registry.addHandler(new ExecuteEditActionHandler(this));
        registry.addHandler(new ChangePropertyHandler(this));
        registry.addHandler(new ContinueInvocationHandler(this));
        registry.addHandler(new ThrowInInvocationHandler(this));
        registry.addHandler(new ClosePressedHandler(this));
        registry.addHandler(new FormHiddenHandler(this));
        registry.addHandler(new OkPressedHandler(this));
        registry.addHandler(new ExecuteNavigatorActionHandler(this));
        registry.addHandler(new ContinueNavigatorActionHandler(this));
        registry.addHandler(new ThrowInNavigatorActionHandler(this));
        registry.addHandler(new SetUserFiltersHandler(this));
        registry.addHandler(new CountRecordsHandler(this));
        registry.addHandler(new CalculateSumHandler(this));
        registry.addHandler(new GroupReportHandler(this));
        registry.addHandler(new PasteExternalTableHandler(this));
        registry.addHandler(new PasteSingleCellValueHandler(this));
        registry.addHandler(new ChangePageSizeHandler(this));
        registry.addHandler(new GetInitialFilterPropertyHandler(this));
        registry.addHandler(new GetRemoteActionMessageHandler(this));
        registry.addHandler(new IsConfigurationAccessAllowedHandler(this));
        registry.addHandler(new ForbidDuplicateFormsHandler(this));
        registry.addHandler(new SaveUserPreferencesActionHandler(this));
        registry.addHandler(new ClientMessageHandler(this));
        registry.addHandler(new ClientResponseHandler(this));
    }

    public FormSessionManager getFormSessionManager() {
        return formSessionManager;
    }
}
