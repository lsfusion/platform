package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.controller.remote.action.form.SetRegularFilter;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.order.Scroll;
import lsfusion.interop.form.remote.RemoteFormInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class SetRegularFilterHandler extends FormServerResponseActionHandler<SetRegularFilter> {
    public SetRegularFilterHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(final SetRegularFilter action, ExecutionContext context) throws RemoteException {
        return getServerResponseResult(action, new RemoteCall() {
            public ServerResponse call(RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.setRegularFilter(action.requestIndex, action.lastReceivedRequestIndex, action.groupId, action.filterId);
            }
        });
    }
}
