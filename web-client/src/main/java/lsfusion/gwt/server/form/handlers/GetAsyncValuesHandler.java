package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.base.exception.AppServerNotAvailableDispatchException;
import lsfusion.gwt.client.base.result.ListResult;
import lsfusion.gwt.client.controller.remote.action.form.GetAsyncValues;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.FormActionHandler;
import lsfusion.http.provider.form.FormSessionObject;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;

public class GetAsyncValuesHandler extends FormActionHandler<GetAsyncValues, ListResult> {
    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public GetAsyncValuesHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    protected String getActionDetails(GetAsyncValues action) {
        return null; // too many logs
    }

    @Override
    public ListResult executeEx(GetAsyncValues action, ExecutionContext context) throws RemoteException, AppServerNotAvailableDispatchException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        String[] asyncValues = form.remoteForm.getAsyncValues(action.requestIndex, action.lastReceivedRequestIndex, action.propertyID, (byte[]) gwtConverter.convertOrCast(action.columnKey), action.actionSID, action.value, action.index);
        if(asyncValues == null)
            return new ListResult(null);
        return new ListResult(new ArrayList(Arrays.asList(asyncValues)));
    }
}