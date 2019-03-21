package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.gwt.client.controller.remote.action.form.RefreshUPHiddenPropsAction;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class RefreshUPHiddenPropsActionHandler extends FormServerResponseActionHandler<RefreshUPHiddenPropsAction> {

    public RefreshUPHiddenPropsActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(RefreshUPHiddenPropsAction action, ExecutionContext context) throws RemoteException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return getServerResponseResult(form, form.remoteForm.refreshUPHiddenProperties(action.requestIndex, defaultLastReceivedRequestIndex, action.groupObjectSID, action.propSids));
    }
}
