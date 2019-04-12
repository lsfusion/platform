package lsfusion.gwt.server.form.handlers;

import com.google.common.base.Throwables;
import lsfusion.gwt.client.controller.remote.action.form.ChangePropertyOrder;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.http.provider.SessionInvalidatedException;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.interop.form.remote.RemoteFormInterface;
import lsfusion.interop.action.ServerResponse;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.io.IOException;
import java.rmi.RemoteException;

public class ChangePropertyOrderHandler extends FormServerResponseActionHandler<ChangePropertyOrder> {
    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public ChangePropertyOrderHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(final ChangePropertyOrder action, ExecutionContext context) throws RemoteException {
        return getServerResponseResult(action, new RemoteCall() {
            public ServerResponse call(RemoteFormInterface remoteForm) throws RemoteException {
                byte[] keyBytes = gwtConverter.convertOrCast(action.columnKey);
                return remoteForm.changePropertyOrder(action.requestIndex, action.lastReceivedRequestIndex, action.propertyID, action.modiType.serialize(), keyBytes);
            }
        });
    }
}
