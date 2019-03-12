package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.shared.actions.form.ChangeClassView;
import lsfusion.gwt.shared.actions.form.ServerResponseResult;
import lsfusion.gwt.shared.view.GClassViewType;
import lsfusion.interop.form.property.ClassViewType;

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
                form.remoteForm.changeClassView(action.requestIndex, defaultLastReceivedRequestIndex, action.groupObjectId, convertClassView(action.newClassView))
        );
    }

    private ClassViewType convertClassView(GClassViewType newClassView) {
        return GwtToClientConverter.getInstance().convertOrNull(newClassView);
    }
}
