package platform.gwt.form.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.client.logics.ClientGroupObject;
import platform.client.logics.ClientObject;
import platform.gwt.base.server.FormSessionObject;
import platform.gwt.form.server.RemoteFormServiceImpl;
import platform.gwt.form.shared.actions.form.ChangeGroupObject;
import platform.gwt.form.shared.actions.form.FormChangesResult;

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
        FormSessionObject form = getFormSessionObject(action.formSessionID);

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream outStream = new DataOutputStream(byteStream);

        ClientGroupObject group = form.clientForm.getGroupObject(action.groupId);
        outStream.writeInt(group.objects.size());
        int i = 0;
        for (ClientObject object : group.objects) {
            outStream.writeInt(object.getID());
            serializeObject(outStream, action.keyValues[i++].getValue());
        }

        form.remoteForm.changeGroupObject(action.groupId, byteStream.toByteArray());

        return getRemoteChanges(form);
    }
}
