package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.RefreshUPHiddenPropsAction;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.remote.RemoteFormInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class RefreshUPHiddenPropsActionHandler extends FormServerResponseActionHandler<RefreshUPHiddenPropsAction> {

    public RefreshUPHiddenPropsActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(final RefreshUPHiddenPropsAction action, ExecutionContext context) throws RemoteException {
        return getServerResponseResult(action, new RemoteCall() {
            public ServerResponse call(RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.refreshUPHiddenProperties(action.requestIndex, action.lastReceivedRequestIndex, action.groupObjectSID, action.propSids);
            }
        });
    }
}
