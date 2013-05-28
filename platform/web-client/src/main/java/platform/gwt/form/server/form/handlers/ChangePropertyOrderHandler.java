package platform.gwt.form.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.form.server.FormDispatchServlet;
import platform.gwt.form.server.FormSessionObject;
import platform.gwt.form.server.convert.GwtToClientConverter;
import platform.gwt.form.shared.actions.form.ChangePropertyOrder;
import platform.gwt.form.shared.actions.form.ServerResponseResult;

import java.io.IOException;

public class ChangePropertyOrderHandler extends ServerResponseActionHandler<ChangePropertyOrder> {
    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public ChangePropertyOrderHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ChangePropertyOrder action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        byte[] keyBytes = gwtConverter.convertOrCast(action.columnKey);
        return getServerResponseResult(
                form,
                form.remoteForm.changePropertyOrder(action.requestIndex, action.propertyID, action.modiType.serialize(), keyBytes)
        );
    }
}
