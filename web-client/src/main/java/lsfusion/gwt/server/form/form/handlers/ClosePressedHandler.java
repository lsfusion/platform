package lsfusion.gwt.server.form.form.handlers;

import lsfusion.gwt.server.form.LSFusionDispatchServlet;
import lsfusion.gwt.server.form.form.FormServerResponseActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import lsfusion.gwt.server.form.form.provider.FormSessionObject;
import lsfusion.gwt.shared.form.actions.form.ClosePressed;
import lsfusion.gwt.shared.form.actions.form.ServerResponseResult;

import java.io.IOException;

public class ClosePressedHandler extends FormServerResponseActionHandler<ClosePressed> {
    public ClosePressedHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ClosePressed action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return getServerResponseResult(form, form.remoteForm.closedPressed(action.requestIndex, defaultLastReceivedRequestIndex));
    }
}
