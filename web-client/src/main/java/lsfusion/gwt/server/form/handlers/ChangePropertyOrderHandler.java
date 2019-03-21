package lsfusion.gwt.server.form.handlers;

import com.google.common.base.Throwables;
import lsfusion.gwt.client.controller.remote.action.form.ChangePropertyOrder;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.http.provider.form.FormSessionObject;
import net.customware.gwt.dispatch.server.ExecutionContext;

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
