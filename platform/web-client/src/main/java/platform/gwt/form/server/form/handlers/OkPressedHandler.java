package platform.gwt.form.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.form.server.FormSessionObject;
import platform.gwt.form.server.RemoteServiceImpl;
import platform.gwt.form.shared.actions.form.OkPressed;
import platform.gwt.form.shared.actions.form.ServerResponseResult;

import java.io.IOException;

public class OkPressedHandler extends ServerResponseActionHandler<OkPressed> {
    public OkPressedHandler(RemoteServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(OkPressed action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return getServerResponseResult(form, form.remoteForm.okPressed(action.requestIndex));
    }
}
