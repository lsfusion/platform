package lsfusion.gwt.form.server.form.handlers;

import lsfusion.gwt.form.server.LSFusionDispatchServlet;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import lsfusion.gwt.form.server.FormSessionObject;
import lsfusion.gwt.form.shared.actions.form.ClearPropertyOrders;
import lsfusion.gwt.form.shared.actions.form.ServerResponseResult;

import java.io.IOException;

public class ClearPropertyOrdersHandler extends ServerResponseActionHandler<ClearPropertyOrders> {
    public ClearPropertyOrdersHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ClearPropertyOrders action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return getServerResponseResult(
                form,
                form.remoteForm.clearPropertyOrders(action.requestIndex, defaultLastReceivedRequestIndex, action.groupObjectID)
        );
    }
}
