package platform.gwt.form.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.client.logics.ClientGroupObject;
import platform.client.logics.ClientObject;
import platform.gwt.base.server.FormSessionObject;
import platform.gwt.form.server.RemoteFormServiceImpl;
import platform.gwt.form.shared.actions.form.FormChangesResult;
import platform.gwt.form.shared.actions.form.AdjustGroupObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static platform.base.BaseUtils.serializeObject;

public class AdjustGroupObjectHandler extends FormChangesActionHandler<AdjustGroupObject> {
    public AdjustGroupObjectHandler(RemoteFormServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public FormChangesResult executeEx(AdjustGroupObject action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream outStream = new DataOutputStream(byteStream);

        ClientGroupObject group = form.clientForm.getGroupObject(action.groupObjectId);
        outStream.writeInt(group.objects.size());
        int i = 0;
        for (ClientObject object : group.objects) {
            outStream.writeInt(object.getID());
            serializeObject(outStream, action.value[i++].getValue());
        }

        form.remoteForm.adjustGroupObject(action.groupObjectId, byteStream.toByteArray());

        return getRemoteChanges(form);
    }
}
