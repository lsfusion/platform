package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.base.GAsync;
import lsfusion.gwt.client.base.exception.AppServerNotAvailableDispatchException;
import lsfusion.gwt.client.base.result.ListResult;
import lsfusion.gwt.client.controller.remote.action.form.GetAsyncValues;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.ClientActionToGwtConverter;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.FormActionHandler;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.interop.form.property.cell.Async;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;

public class GetAsyncValuesHandler extends FormActionHandler<GetAsyncValues, ListResult> {
    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();
    private static ClientActionToGwtConverter clientActionConverter = ClientActionToGwtConverter.getInstance();

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
        return convertAsyncValues(form.remoteForm.getAsyncValues(action.requestIndex, action.lastReceivedRequestIndex, action.propertyID, (byte[]) gwtConverter.convertOrCast(action.columnKey), action.actionSID, action.value, action.index));
    }

    public static ListResult convertAsyncValues(Async[] asyncValues) {
        if(asyncValues == null)
            return new ListResult(null);

        ArrayList<GAsync> gAsyncValues = new ArrayList<>();
        for(Async asyncValue : asyncValues)
            gAsyncValues.add(clientActionConverter.convertAsync(asyncValue));
        return new ListResult(gAsyncValues);
    }
}