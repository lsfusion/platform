package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.SelectAll;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.remote.RemoteFormInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class SelectAllHandler extends FormServerResponseActionHandler<SelectAll> {
    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public SelectAllHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(final SelectAll action, ExecutionContext context) throws RemoteException {
        return getServerResponseResult(action, remoteForm -> {
            int size = action.changeSelectionProps.length;
            byte[][] changeSelectionColumnKeys = new byte[size][];
            for (int i = 0; i < size; i++) {
                changeSelectionColumnKeys[i] = gwtConverter.convertOrCast(action.changeSelectionColumnKeys[i]);
            }

            return remoteForm.selectAll(action.requestIndex, action.lastReceivedRequestIndex, action.groupId,
                action.changeSelectionProps, changeSelectionColumnKeys, action.changeSelectionValues);
        });
    }
}
