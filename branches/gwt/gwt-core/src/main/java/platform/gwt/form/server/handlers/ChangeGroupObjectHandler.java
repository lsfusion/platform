package platform.gwt.form.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.client.logics.ClientFormChanges;
import platform.gwt.base.server.DebugUtil;
import platform.gwt.base.shared.MessageException;
import platform.gwt.form.server.FormSessionObject;
import platform.gwt.form.server.RemoteFormServiceImpl;
import platform.gwt.form.shared.actions.form.ChangeGroupObject;
import platform.gwt.form.shared.actions.form.FormChangesResult;
import platform.gwt.view.changes.dto.ObjectDTO;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import static platform.base.BaseUtils.serializeObject;

public class ChangeGroupObjectHandler extends FormServiceActionHandler<ChangeGroupObject, FormChangesResult> {
    public ChangeGroupObjectHandler(RemoteFormServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public FormChangesResult execute(ChangeGroupObject action, ExecutionContext context) throws DispatchException {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            DataOutputStream outStream = new DataOutputStream(byteStream);
            for (ObjectDTO keyValue : action.keyValues) {
                serializeObject(outStream, keyValue.getValue());
            }

            FormSessionObject form = getSessionFormExceptionally(action.formSessionID);

            form.remoteForm.changeGroupObject(action.groupId, byteStream.toByteArray());

            //todo: create base handler class to return remote changes...
            ClientFormChanges clientChanges = new ClientFormChanges(new DataInputStream(new ByteArrayInputStream(form.remoteForm.getRemoteChanges().form)), form.clientForm, null);
            return new FormChangesResult(clientChanges.getGwtFormChangesDTO());
        } catch (Throwable e) {
            logger.error("Ошибка в changeGroupObject: ", e);
            e.printStackTrace();
            throw new MessageException(DebugUtil.getInitialCause(e).getMessage());
        }
    }
}
