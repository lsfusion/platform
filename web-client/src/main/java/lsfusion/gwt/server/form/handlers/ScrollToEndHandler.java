package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.ScrollToEnd;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.interop.form.order.Scroll;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class ScrollToEndHandler extends FormServerResponseActionHandler<ScrollToEnd> {
    public ScrollToEndHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ScrollToEnd action, ExecutionContext context) throws RemoteException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        Scroll scrollType = action.toEnd ? Scroll.END : Scroll.HOME;
        return getServerResponseResult(form,
                                       form.remoteForm.changeGroupObject(action.requestIndex, defaultLastReceivedRequestIndex, action.groupId, scrollType.serialize()));
    }
}
