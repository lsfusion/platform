package lsfusion.gwt.server.form.handlers;

import com.google.common.base.Throwables;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.shared.actions.form.ChangePropertyOrder;
import lsfusion.gwt.shared.actions.form.ServerResponseResult;

import java.io.IOException;

public class ChangePropertyOrderHandler extends FormServerResponseActionHandler<ChangePropertyOrder> {
    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public ChangePropertyOrderHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ChangePropertyOrder action, ExecutionContext context) {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        byte[] keyBytes = gwtConverter.convertOrCast(action.columnKey);
        try {
            return getServerResponseResult(
                    form,
                    form.remoteForm.changePropertyOrder(action.requestIndex, defaultLastReceivedRequestIndex, action.propertyID, action.modiType.serialize(), keyBytes)
            );
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
}
