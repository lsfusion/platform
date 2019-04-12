package lsfusion.gwt.server.form.handlers;

import com.google.common.base.Throwables;
import lsfusion.gwt.client.controller.remote.action.form.ChangeProperty;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.http.provider.SessionInvalidatedException;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.remote.RemoteFormInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.io.IOException;
import java.rmi.RemoteException;

import static lsfusion.base.BaseUtils.serializeObject;

public class ChangePropertyHandler extends FormServerResponseActionHandler<ChangeProperty> {

    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public ChangePropertyHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(final ChangeProperty action, ExecutionContext context) throws RemoteException {
        return getServerResponseResult(action, new RemoteCall() {
            public ServerResponse call(RemoteFormInterface remoteForm) throws RemoteException {
                Object value = gwtConverter.convertOrCast(action.value);
                byte[] fullKey = gwtConverter.convertOrCast(action.fullKey);
                byte[] pushChange;
                try {
                    pushChange = serializeObject(gwtConverter.convertOrCast(value));
                } catch (IOException e) {
                    throw Throwables.propagate(e);
                }
                return remoteForm.changeProperty(
                        action.requestIndex,
                        action.lastReceivedRequestIndex,
                        action.propertyId,
                        fullKey,
                        pushChange,
                        action.addedObjectId
                );
            }
        });
    }
}
