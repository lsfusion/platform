package platform.gwt.form2.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.base.server.FormSessionObject;
import platform.gwt.form2.server.RemoteServiceImpl;
import platform.gwt.form2.shared.actions.form.ClosePressed;
import platform.gwt.form2.shared.actions.form.ServerResponseResult;

import java.io.IOException;

public class ClosePressedHandler extends ServerResponseActionHandler<ClosePressed> {
    public ClosePressedHandler(RemoteServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ClosePressed action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return getServerResponseResult(form, form.remoteForm.closedPressed(action.requestIndex));
    }
}
