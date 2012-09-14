package platform.gwt.form2.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.base.server.FormSessionObject;
import platform.gwt.form2.server.RemoteServiceImpl;
import platform.gwt.form2.shared.actions.form.ClearPropertyOrders;
import platform.gwt.form2.shared.actions.form.ServerResponseResult;

import java.io.IOException;

public class ClearPropertyOrdersHandler extends ServerResponseActionHandler<ClearPropertyOrders> {
    public ClearPropertyOrdersHandler(RemoteServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ClearPropertyOrders action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return getServerResponseResult(
                form,
                form.remoteForm.clearPropertyOrders(action.requestIndex, action.groupObjectID)
        );
    }
}
