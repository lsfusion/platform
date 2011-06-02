package platform.gwt.form.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.client.logics.ClientFormChanges;
import platform.gwt.base.server.DebugUtil;
import platform.gwt.base.shared.MessageException;
import platform.gwt.form.server.FormSessionObject;
import platform.gwt.form.server.RemoteFormServiceImpl;
import platform.gwt.form.shared.actions.form.FormChangesResult;
import platform.gwt.form.shared.actions.form.GetRemoteChanges;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

public class GetRemoteChangesHandler extends FormServiceActionHandler<GetRemoteChanges, FormChangesResult> {
    public GetRemoteChangesHandler(RemoteFormServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public FormChangesResult execute(GetRemoteChanges action, ExecutionContext context) throws DispatchException {
        try {
            FormSessionObject form = getSessionFormExceptionally(action.formSessionID);

//            remoteForm.refreshData();
            ClientFormChanges clientChanges = new ClientFormChanges(new DataInputStream(new ByteArrayInputStream(form.remoteForm.getRemoteChanges().form)), form.clientForm, null);
            return new FormChangesResult(clientChanges.getGwtFormChangesDTO());
        } catch (Throwable e) {
            logger.error("Ошибка в getRemoteChanges: ", e);
            e.printStackTrace();
            throw new MessageException(DebugUtil.getInitialCause(e).getMessage());
        }
    }
}
