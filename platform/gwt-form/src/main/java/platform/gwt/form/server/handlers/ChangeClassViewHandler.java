package platform.gwt.form.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.base.server.FormSessionObject;
import platform.gwt.form.server.RemoteFormServiceImpl;
import platform.gwt.form.shared.actions.form.ChangeClassView;
import platform.gwt.form.shared.actions.form.FormChangesResult;

import java.io.IOException;

public class ChangeClassViewHandler extends FormChangesActionHandler<ChangeClassView> {
    public ChangeClassViewHandler(RemoteFormServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public FormChangesResult executeEx(ChangeClassView action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);

        form.remoteForm.changeClassView(action.groupObjectId, (String) action.value.getValue());

        return getRemoteChanges(form);
    }
}
