package lsfusion.gwt.form.server.form.handlers;

import lsfusion.gwt.form.server.LSFusionDispatchServlet;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import lsfusion.gwt.form.server.FormSessionObject;
import lsfusion.gwt.form.shared.actions.form.ClosePressed;
import lsfusion.gwt.form.shared.actions.form.ServerResponseResult;

import java.io.IOException;

public class ClosePressedHandler extends ServerResponseActionHandler<ClosePressed> {
    public ClosePressedHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ClosePressed action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return getServerResponseResult(form, form.remoteForm.closedPressed(action.requestIndex, defaultLastReceivedRequestIndex));
    }
}
