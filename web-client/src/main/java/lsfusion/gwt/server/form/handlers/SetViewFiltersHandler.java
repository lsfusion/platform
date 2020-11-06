package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.controller.remote.action.form.SetViewFilters;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class SetViewFiltersHandler extends FormServerResponseActionHandler<SetViewFilters> {

    public SetViewFiltersHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(final SetViewFilters action, ExecutionContext context) throws RemoteException {
        return getServerResponseResult(action, remoteForm ->
                remoteForm.setViewFilters(action.requestIndex, action.lastReceivedRequestIndex, SetUserFiltersHandler.serializeFilters(action.filters)));
    }
}
