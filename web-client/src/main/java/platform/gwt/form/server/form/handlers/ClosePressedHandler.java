package platform.gwt.form.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.form.server.FormDispatchServlet;
import platform.gwt.form.server.FormSessionObject;
import platform.gwt.form.shared.actions.form.ClosePressed;
import platform.gwt.form.shared.actions.form.ServerResponseResult;

import java.io.IOException;

public class ClosePressedHandler extends ServerResponseActionHandler<ClosePressed> {
    public ClosePressedHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ClosePressed action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return getServerResponseResult(form, form.remoteForm.closedPressed(action.requestIndex));
    }
}
