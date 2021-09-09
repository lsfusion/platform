package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.ExecuteEventAction;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.async.GPushAsyncResult;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class ExecuteEventActionHandler extends FormServerResponseActionHandler<ExecuteEventAction> {
    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public ExecuteEventActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(final ExecuteEventAction action, ExecutionContext context) throws RemoteException {
        return getServerResponseResult(action, remoteForm -> {
            GGroupObjectValue[] actionFullKeys = action.fullKeys;
            GPushAsyncResult[] actionPushAsyncResults = action.pushAsyncResults;
            int length = actionFullKeys.length;

            byte[][] fullKeys = new byte[length][];
            byte[][] pushAsyncResults = new byte[length][];
            for (int i = 0; i < length; i++) {
                fullKeys[i] = gwtConverter.convertOrCast(actionFullKeys[i]);
                pushAsyncResults[i] = gwtConverter.convertOrCast(actionPushAsyncResults[i]);
            }
            return remoteForm.executeEventAction(
                    action.requestIndex,
                    action.lastReceivedRequestIndex,
                    action.actionSID,
                    action.propertyIds,
                    fullKeys,
                    action.externalChanges,
                    pushAsyncResults
            );
        });
    }
}
