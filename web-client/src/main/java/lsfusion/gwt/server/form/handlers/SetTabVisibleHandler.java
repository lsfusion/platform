package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.controller.remote.action.form.SetTabVisible;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.remote.RemoteFormInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class SetTabVisibleHandler extends FormServerResponseActionHandler<SetTabVisible> {
    public SetTabVisibleHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(final SetTabVisible action, ExecutionContext context) throws RemoteException {
        return getServerResponseResult(action, new RemoteCall() {
            public ServerResponse call(RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.setTabVisible(action.requestIndex, action.lastReceivedRequestIndex, action.tabbedPaneID, action.tabIndex);
            }
        });
    }
}
