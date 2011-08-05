package platform.gwt.form.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.base.server.FormSessionObject;
import platform.gwt.form.server.RemoteFormServiceImpl;
import platform.gwt.form.shared.actions.form.CancelChanges;
import platform.gwt.form.shared.actions.form.FormChangesResult;

import java.io.IOException;

public class CancelChangesHandler extends FormChangesActionHandler<CancelChanges> {
    public CancelChangesHandler(RemoteFormServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public FormChangesResult executeEx(CancelChanges action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);

        form.remoteForm.cancelChanges();

        return getRemoteChanges(form);
    }
}
