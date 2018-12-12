package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import lsfusion.gwt.server.LSFusionDispatchServlet;
import lsfusion.gwt.server.form.provider.FormSessionObject;
import lsfusion.gwt.shared.actions.form.OkPressed;
import lsfusion.gwt.shared.actions.form.ServerResponseResult;

import java.io.IOException;

public class OkPressedHandler extends FormServerResponseActionHandler<OkPressed> {
    public OkPressedHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(OkPressed action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return getServerResponseResult(form, form.remoteForm.okPressed(action.requestIndex, defaultLastReceivedRequestIndex));
    }
}
