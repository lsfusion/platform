package platform.gwt.form.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.base.server.FormSessionObject;
import platform.gwt.form.server.RemoteFormServiceImpl;
import platform.gwt.form.shared.actions.form.FormChangesResult;
import platform.gwt.form.shared.actions.form.SetRegularFilter;

import java.io.IOException;

public class SetRegularFilterHandler extends FormChangesActionHandler<SetRegularFilter> {
    public SetRegularFilterHandler(RemoteFormServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public FormChangesResult executeEx(SetRegularFilter action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getSessionFormExceptionally(action.formSessionID);
        form.remoteForm.setRegularFilter(action.groupId, action.filterId);

        return getRemoteChanges(form);
    }
}
