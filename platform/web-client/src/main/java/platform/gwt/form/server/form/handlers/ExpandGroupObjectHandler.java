package platform.gwt.form.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.base.BaseUtils;
import platform.gwt.form.server.FormSessionObject;
import platform.gwt.form.server.RemoteServiceImpl;
import platform.gwt.form.shared.actions.form.ExpandGroupObject;
import platform.gwt.form.shared.actions.form.ServerResponseResult;

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
        for (Map.Entry<Integer, Object> one : action.value.entrySet()) {
            outStream.writeInt(one.getKey());
            BaseUtils.serializeObject(outStream, one.getValue());
        }

        return getServerResponseResult(form, form.remoteForm.expandGroupObject(action.requestIndex, action.groupObjectId, byteStream.toByteArray()));
    }
}
