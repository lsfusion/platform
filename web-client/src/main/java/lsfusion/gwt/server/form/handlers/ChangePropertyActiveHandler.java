package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.controller.remote.action.form.ChangePropertyActive;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.event.ChangeSelection;
import lsfusion.interop.form.remote.RemoteFormInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class ChangePropertyActiveHandler extends FormServerResponseActionHandler<ChangePropertyActive> {
    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public ChangePropertyActiveHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(final ChangePropertyActive action, ExecutionContext context) throws RemoteException {
        return getServerResponseResult(action, new RemoteCall() {
            public ServerResponse call(RemoteFormInterface remoteForm) throws RemoteException {
                byte[][] changeSelectionColumnKeys = null;

                if(action.changeSelectionProps != null) {
                    int size = action.changeSelectionProps.length;
                    changeSelectionColumnKeys = new byte[size][];
                    for (int i = 0; i < size; i++) {
                        changeSelectionColumnKeys[i] = gwtConverter.convertOrCast(action.changeSelectionColumnKeys[i]);
                    }
                }

                return remoteForm.changePropertyActive(action.requestIndex, action.lastReceivedRequestIndex, action.propertyId,
                        gwtConverter.convertOrCast(action.columnKey), action.focused, action.changeSelectionProps, changeSelectionColumnKeys, action.changeSelectionValues);
            }
        });
    }
}