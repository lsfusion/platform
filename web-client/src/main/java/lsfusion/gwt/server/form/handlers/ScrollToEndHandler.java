package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.ScrollToEnd;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.order.Scroll;
import lsfusion.interop.form.remote.RemoteFormInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class ScrollToEndHandler extends FormServerResponseActionHandler<ScrollToEnd> {
    public ScrollToEndHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(final ScrollToEnd action, ExecutionContext context) throws RemoteException {
        return getServerResponseResult(action, new RemoteCall() {
            public ServerResponse call(RemoteFormInterface remoteForm) throws RemoteException {
                Scroll scrollType = action.toEnd ? Scroll.END : Scroll.HOME;
                return remoteForm.changeGroupObject(action.requestIndex, action.lastReceivedRequestIndex, action.groupId, scrollType.serialize());
            }
        });
    }
}
