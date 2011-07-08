package platform.gwt.form.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.base.server.FormSessionObject;
import platform.gwt.form.server.RemoteFormServiceImpl;
import platform.gwt.form.shared.actions.form.ChangeGroupObject;
import platform.gwt.form.shared.actions.form.FormChangesResult;
import platform.gwt.view.changes.dto.ObjectDTO;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static platform.base.BaseUtils.serializeObject;

public class ChangeGroupObjectHandler extends FormChangesActionHandler<ChangeGroupObject> {
    public ChangeGroupObjectHandler(RemoteFormServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public FormChangesResult executeEx(ChangeGroupObject action, ExecutionContext context) throws DispatchException, IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream outStream = new DataOutputStream(byteStream);
        for (ObjectDTO keyValue : action.keyValues) {
            serializeObject(outStream, keyValue.getValue());
        }

        FormSessionObject form = getSessionFormExceptionally(action.formSessionID);

        form.remoteForm.changeGroupObject(action.groupId, byteStream.toByteArray());

        return getRemoteChanges(form);
    }
}
