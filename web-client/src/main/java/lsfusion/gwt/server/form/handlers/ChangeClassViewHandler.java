package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.ChangeClassView;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.form.property.GClassViewType;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.interop.form.property.ClassViewType;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class ChangeClassViewHandler extends FormServerResponseActionHandler<ChangeClassView> {
    public ChangeClassViewHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ChangeClassView action, ExecutionContext context) throws RemoteException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return getServerResponseResult(
                form,
                form.remoteForm.changeClassView(action.requestIndex, action.lastReceivedRequestIndex, action.groupObjectId, convertClassView(action.newClassView))
        );
    }

    private ClassViewType convertClassView(GClassViewType newClassView) {
        return GwtToClientConverter.getInstance().convertOrNull(newClassView);
    }
}
