package lsfusion.gwt.form.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import lsfusion.gwt.form.server.FormDispatchServlet;
import lsfusion.gwt.form.server.FormSessionObject;
import lsfusion.gwt.form.server.convert.GwtToClientConverter;
import lsfusion.gwt.form.shared.actions.form.ExecuteEditAction;
import lsfusion.gwt.form.shared.actions.form.ServerResponseResult;

import java.io.IOException;

public class ExecuteEditActionHandler extends ServerResponseActionHandler<ExecuteEditAction> {
    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public ExecuteEditActionHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ExecuteEditAction action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);

        byte[] columnKey = gwtConverter.convertOrCast(action.columnKey);

        return getServerResponseResult(form, form.remoteForm.executeEditAction(action.requestIndex, -1, action.propertyId, columnKey, action.actionSID));
    }
}
