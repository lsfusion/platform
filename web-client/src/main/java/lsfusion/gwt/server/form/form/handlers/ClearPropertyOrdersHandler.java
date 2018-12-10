package lsfusion.gwt.form.server.form.handlers;

import lsfusion.gwt.form.server.spring.LSFusionDispatchServlet;
import lsfusion.gwt.form.server.form.FormServerResponseActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import lsfusion.gwt.form.server.form.spring.FormSessionObject;
import lsfusion.gwt.form.shared.actions.form.ClearPropertyOrders;
import lsfusion.gwt.form.shared.actions.form.ServerResponseResult;

import java.io.IOException;

public class ClearPropertyOrdersHandler extends FormServerResponseActionHandler<ClearPropertyOrders> {
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
