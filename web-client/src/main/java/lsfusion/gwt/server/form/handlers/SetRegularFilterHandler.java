package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.controller.remote.action.form.SetRegularFilter;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.http.provider.form.FormSessionObject;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class SetRegularFilterHandler extends FormServerResponseActionHandler<SetRegularFilter> {
    public SetRegularFilterHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(SetRegularFilter action, ExecutionContext context) throws RemoteException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return getServerResponseResult(form, form.remoteForm.setRegularFilter(action.requestIndex, action.lastReceivedRequestIndex, action.groupId, action.filterId));
    }
}
