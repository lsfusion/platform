package platform.gwt.form.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.base.server.FormSessionObject;
import platform.gwt.form.server.RemoteFormServiceImpl;
import platform.gwt.form.shared.actions.form.ApplyChanges;
import platform.gwt.form.shared.actions.form.FormChangesResult;

import java.io.IOException;

public class ApplyChangesHandler extends FormChangesActionHandler<ApplyChanges> {
    public ApplyChangesHandler(RemoteFormServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public FormChangesResult executeEx(ApplyChanges action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getSessionFormExceptionally(action.formSessionID);

        form.remoteForm.applyChanges();

        return getRemoteChanges(form);
    }
}
