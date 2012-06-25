package platform.gwt.form.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.base.server.FormSessionObject;
import platform.gwt.form.server.RemoteFormServiceImpl;
import platform.gwt.form.shared.actions.form.FormChangesResult;
import platform.gwt.form.shared.actions.form.SetTabVisible;

import java.io.IOException;

public class SetTabVisibleHandler extends FormChangesActionHandler<SetTabVisible> {
    public SetTabVisibleHandler(RemoteFormServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public FormChangesResult executeEx(SetTabVisible action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return getRemoteChanges(form, form.remoteForm.setTabVisible(-1, action.tabbedPaneID, action.tabIndex));
    }
}
