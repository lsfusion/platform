package platform.gwt.form.server.handlers;

import net.customware.gwt.dispatch.shared.Action;
import platform.gwt.base.server.FormSessionObject;
import platform.gwt.form.server.RemoteFormServiceImpl;
import platform.gwt.form.server.dispatch.GWTFormActionDispatcher;
import platform.gwt.form.shared.actions.form.FormChangesResult;
import platform.interop.form.ServerResponse;

import java.io.IOException;

public abstract class FormChangesActionHandler<A extends Action<FormChangesResult>> extends FormServiceActionHandler<A, FormChangesResult> {
    protected FormChangesActionHandler(RemoteFormServiceImpl servlet) {
        super(servlet);
    }

    protected FormChangesResult getRemoteChanges(FormSessionObject form) throws IOException {
        return getRemoteChanges(form, form.remoteForm.getRemoteChanges(-1));
    }

    protected FormChangesResult getRemoteChanges(FormSessionObject form, ServerResponse remoteChanges) throws IOException {
        GWTFormActionDispatcher dispatcher = new GWTFormActionDispatcher(form);
        dispatcher.dispatchResponse(remoteChanges);
        return new FormChangesResult(dispatcher.formChanges);
    }
}
