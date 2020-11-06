package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.controller.remote.action.form.SetRegularFilter;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class SetRegularFilterHandler extends FormServerResponseActionHandler<SetRegularFilter> {
    public SetRegularFilterHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(final SetRegularFilter action, ExecutionContext context) throws RemoteException {
        return getServerResponseResult(action, remoteForm ->
                remoteForm.setRegularFilter(action.requestIndex, action.lastReceivedRequestIndex, action.groupId, action.filterId));
    }
}
