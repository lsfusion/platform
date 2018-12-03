package lsfusion.gwt.form.server.form.handlers;

import lsfusion.gwt.form.server.LSFusionDispatchServlet;
import lsfusion.gwt.form.server.FormSessionObject;
import lsfusion.gwt.form.shared.actions.form.ExecuteNotification;
import lsfusion.gwt.form.shared.actions.form.ServerResponseResult;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class ExecuteNotificationHandler extends ServerResponseActionHandler<ExecuteNotification> {
    public ExecuteNotificationHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ExecuteNotification action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return getServerResponseResult(form, form.remoteForm.executeNotificationAction(action.requestIndex, defaultLastReceivedRequestIndex, action.idNotification));
    }
}