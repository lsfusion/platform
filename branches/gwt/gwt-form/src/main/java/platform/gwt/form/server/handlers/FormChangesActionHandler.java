package platform.gwt.form.server.handlers;

import net.customware.gwt.dispatch.shared.Action;
import platform.client.logics.ClientFormChanges;
import platform.gwt.form.server.FormSessionObject;
import platform.gwt.form.server.RemoteFormServiceImpl;
import platform.gwt.form.shared.actions.form.FormChangesResult;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public abstract class FormChangesActionHandler<A extends Action<FormChangesResult>> extends FormServiceActionHandler<A, FormChangesResult> {
    protected FormChangesActionHandler(RemoteFormServiceImpl servlet) {
        super(servlet);
    }

    protected FormChangesResult getRemoteChanges(FormSessionObject form) throws IOException {
        ClientFormChanges clientChanges =
                new ClientFormChanges(new DataInputStream(new ByteArrayInputStream(form.remoteForm.getRemoteChanges().form)), form.clientForm, null);
        return new FormChangesResult(clientChanges.getGwtFormChangesDTO());
    }
}
