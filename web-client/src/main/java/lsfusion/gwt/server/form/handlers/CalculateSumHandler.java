package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.FormActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;
import lsfusion.gwt.client.base.result.NumberResult;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.client.controller.remote.action.form.CalculateSum;

import java.rmi.RemoteException;

public class CalculateSumHandler extends FormActionHandler<CalculateSum, NumberResult> {
    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public CalculateSumHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public NumberResult executeEx(CalculateSum action, ExecutionContext context) throws RemoteException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return new NumberResult((Number) form.remoteForm.calculateSum(action.requestIndex, defaultLastReceivedRequestIndex, action.propertyID, (byte[]) gwtConverter.convertOrCast(action.columnKey)));
    }
}
