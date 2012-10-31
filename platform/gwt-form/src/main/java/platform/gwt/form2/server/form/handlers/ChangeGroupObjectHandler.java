package platform.gwt.form2.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.form2.server.FormSessionObject;
import platform.gwt.form2.server.RemoteServiceImpl;
import platform.gwt.form2.shared.actions.form.ChangeGroupObject;
import platform.gwt.form2.shared.actions.form.ServerResponseResult;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

import static platform.base.BaseUtils.serializeObject;

public class ChangeGroupObjectHandler extends ServerResponseActionHandler<ChangeGroupObject> {
    public ChangeGroupObjectHandler(RemoteServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ChangeGroupObject action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream outStream = new DataOutputStream(byteStream);

        outStream.writeInt(action.keyValues.size());
        for (Map.Entry<Integer, Object> entry : action.keyValues.entrySet()) {
            outStream.writeInt(entry.getKey());
            serializeObject(outStream, entry.getValue());
        }

        return getServerResponseResult(form, form.remoteForm.changeGroupObject(action.requestIndex, action.groupId, byteStream.toByteArray()));
    }
}
