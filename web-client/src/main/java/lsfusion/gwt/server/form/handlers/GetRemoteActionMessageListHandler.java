package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.base.result.ListResult;
import lsfusion.gwt.client.controller.remote.action.form.GetRemoteActionMessageList;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.ClientActionToGwtConverter;
import lsfusion.gwt.server.form.FormActionHandler;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.interop.ProgressBar;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class GetRemoteActionMessageListHandler extends FormActionHandler<GetRemoteActionMessageList, ListResult> {
    private static ClientActionToGwtConverter clientActionConverter = ClientActionToGwtConverter.getInstance();

    public GetRemoteActionMessageListHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    protected String getActionDetails(GetRemoteActionMessageList action) {
        return null; // too many logs
    }

    @Override
    public ListResult executeEx(GetRemoteActionMessageList action, ExecutionContext context) throws RemoteException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        List<Object> result = new ArrayList<>();
        if (form != null) {

            for (Object object : form.remoteForm.getRemoteActionMessageList()) {
                if (object instanceof ProgressBar)
                    result.add(clientActionConverter.convertProgressBar((ProgressBar) object));
                else
                    result.add(object);
            }
        }
        return new ListResult((ArrayList) result);
    }
}