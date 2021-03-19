package lsfusion.gwt.server.form.handlers;

import com.google.common.base.Throwables;
import lsfusion.gwt.client.base.exception.AppServerNotAvailableDispatchException;
import lsfusion.gwt.client.controller.remote.action.form.ChangeProperties;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.remote.RemoteFormInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;

import static lsfusion.base.BaseUtils.serializeObject;

public class ChangePropertiesHandler extends FormServerResponseActionHandler<ChangeProperties> {

    private static final GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public ChangePropertiesHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ChangeProperties action, ExecutionContext context) throws RemoteException, AppServerNotAvailableDispatchException {
        return getServerResponseResult(action, new RemoteCall() {
            public ServerResponse call(RemoteFormInterface remoteForm) throws RemoteException {
                GGroupObjectValue[] actionFullKeys = action.fullKeys;
                int length = actionFullKeys.length;
                byte[][] fullKeys = new byte[length][];

                Serializable[] values = action.values;
                byte[][] pushChanges = new byte[length][];
                for (int i = 0; i < length; i++) {
                    fullKeys[i] = gwtConverter.convertOrCast(actionFullKeys[i]);

                    Serializable value = values[i];
                    Object objectValue = gwtConverter.convertOrCast(value);
                    byte[] pushChange;
                    try {
                        pushChange = serializeObject(gwtConverter.convertOrCast(objectValue));
                    } catch (IOException e) {
                        throw Throwables.propagate(e);
                    }
                    pushChanges[i] = pushChange;
                }

                return remoteForm.changeProperties(
                        action.requestIndex,
                        action.lastReceivedRequestIndex,
                        action.actionSID,
                        action.propertyIds,
                        fullKeys,
                        pushChanges,
                        action.addedObjectsIds
                );
            }
        });
    }
}
