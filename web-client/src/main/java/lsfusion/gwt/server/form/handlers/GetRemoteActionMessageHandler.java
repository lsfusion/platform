package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.server.form.FormActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.general.StringResult;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.gwt.client.controller.remote.action.form.GetRemoteActionMessage;

import java.rmi.RemoteException;

public class GetRemoteActionMessageHandler extends FormActionHandler<GetRemoteActionMessage, StringResult> {
    public GetRemoteActionMessageHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    protected String getActionDetails(GetRemoteActionMessage action) {
        return null; // too many logs
    }

    @Override
    public StringResult executeEx(GetRemoteActionMessage action, ExecutionContext context) throws RemoteException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return new StringResult(form == null ? "" : form.remoteForm.getRemoteActionMessage());
    }
}
