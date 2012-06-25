package platform.gwt.main.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.base.BaseUtils;
import platform.gwt.base.server.FormSessionObject;
import platform.gwt.main.server.RemoteServiceImpl;
import platform.gwt.main.shared.actions.form.ExpandGroupObject;
import platform.gwt.main.shared.actions.form.ServerResponseResult;
import platform.gwt.view.changes.dto.ObjectDTO;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

public class ExpandGroupObjectHandler extends ServerResponseActionHandler<ExpandGroupObject> {
    public ExpandGroupObjectHandler(RemoteServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ExpandGroupObject action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream outStream = new DataOutputStream(byteStream);

        outStream.writeInt(action.value.size());
        for (Map.Entry<Integer, ObjectDTO> one : action.value.entrySet()) {
            outStream.writeInt(one.getKey());
            BaseUtils.serializeObject(outStream, one.getValue().getValue());
        }

        return getServerResponseResult(form, form.remoteForm.expandGroupObject(action.requestIndex, action.groupObjectId, byteStream.toByteArray()));
    }
}
