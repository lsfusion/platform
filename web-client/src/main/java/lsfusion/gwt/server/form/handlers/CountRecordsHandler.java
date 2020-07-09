package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.base.result.NumberResult;
import lsfusion.gwt.client.controller.remote.action.form.CountRecords;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.FormActionHandler;
import lsfusion.http.provider.form.FormSessionObject;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class CountRecordsHandler extends FormActionHandler<CountRecords, NumberResult> {
    public CountRecordsHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public NumberResult executeEx(CountRecords action, ExecutionContext context) throws RemoteException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return new NumberResult(form.remoteForm.countRecords(action.requestIndex, action.lastReceivedRequestIndex, action.groupObjectID));
    }
}