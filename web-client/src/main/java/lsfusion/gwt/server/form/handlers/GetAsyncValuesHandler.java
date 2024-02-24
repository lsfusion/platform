package lsfusion.gwt.server.form.handlers;

import com.google.common.base.Throwables;
import lsfusion.gwt.client.base.GAsync;
import lsfusion.gwt.client.base.exception.AppServerNotAvailableDispatchException;
import lsfusion.gwt.client.base.result.ListResult;
import lsfusion.gwt.client.controller.remote.action.form.GetAsyncValues;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.ClientFormChangesToGwtConverter;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.FormActionHandler;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.client.form.property.cell.ClientAsync;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;

public class GetAsyncValuesHandler extends FormActionHandler<GetAsyncValues, ListResult> {
    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();
    private static ClientFormChangesToGwtConverter clientValuesConverter = ClientFormChangesToGwtConverter.getInstance();

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
        return getAndConvertAsyncValues(form, servlet, action.requestIndex, action.lastReceivedRequestIndex, action.propertyID, (byte[]) gwtConverter.convertOrCast(action.columnKey), action.actionSID, action.value, action.index, 0);
    }

    public static ListResult getAndConvertAsyncValues(FormSessionObject form, MainDispatchServlet servlet, long requestIndex, long lastReceivedRequestIndex, int propertyID, byte[] fullKey, String actionSID, String value, int index, int increaseValuesNeededCount) throws RemoteException {
        return convertAsyncValues(ClientAsync.deserialize(form.remoteForm.getAsyncValues(requestIndex, lastReceivedRequestIndex, propertyID, fullKey, actionSID, value, index, increaseValuesNeededCount), form.clientForm), form, servlet);
    }
    public static ListResult convertAsyncValues(ClientAsync[] asyncValues, FormSessionObject form, MainDispatchServlet servlet) {
        if(asyncValues == null)
            return new ListResult(null);

        ArrayList<GAsync> gAsyncValues = new ArrayList<>();
        for(ClientAsync asyncValue : asyncValues) {
            try {
                gAsyncValues.add(clientValuesConverter.convertAsync(asyncValue, form, servlet));
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }
        return new ListResult(gAsyncValues);
    }
}