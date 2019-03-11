package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.gwt.shared.actions.form.ClosePressed;
import lsfusion.gwt.shared.actions.form.ServerResponseResult;

import java.rmi.RemoteException;

public class ClosePressedHandler extends FormServerResponseActionHandler<ClosePressed> {
    public ClosePressedHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ClosePressed action, ExecutionContext context) throws RemoteException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return getServerResponseResult(form, form.remoteForm.closedPressed(action.requestIndex, defaultLastReceivedRequestIndex));
    }
}
