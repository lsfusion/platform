package platform.gwt.form.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.base.server.FormSessionObject;
import platform.gwt.form.server.RemoteFormServiceImpl;
import platform.gwt.form.shared.actions.form.FormChangesResult;
import platform.gwt.form.shared.actions.form.GetRemoteChanges;

import java.io.IOException;

public class GetRemoteChangesHandler extends FormChangesActionHandler<GetRemoteChanges> {
    public GetRemoteChangesHandler(RemoteFormServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public FormChangesResult executeEx(GetRemoteChanges action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);

        return getRemoteChanges(form, form.remoteForm.refreshPressed());
    }
}
