package lsfusion.gwt.server.navigator.handlers;

import lsfusion.gwt.client.base.result.ListResult;
import lsfusion.gwt.client.controller.remote.action.navigator.GetRemoteNavigatorActionMessageList;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.ClientActionToGwtConverter;
import lsfusion.gwt.server.form.handlers.GetRemoteActionMessageListHandler;
import lsfusion.gwt.server.navigator.NavigatorActionHandler;
import lsfusion.interop.ProgressBar;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class GetRemoteNavigatorActionMessageListHandler extends NavigatorActionHandler<GetRemoteNavigatorActionMessageList, ListResult> {

    public GetRemoteNavigatorActionMessageListHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    protected String getActionDetails(GetRemoteNavigatorActionMessageList action) {
        return null; // too many logs
    }

    @Override
    public ListResult executeEx(GetRemoteNavigatorActionMessageList action, ExecutionContext context) throws RemoteException {
        List<Object> result = new ArrayList<>();
        for (Object object : getRemoteNavigator(action).getRemoteActionMessageList()) {
            if (object instanceof ProgressBar)
                result.add(GetRemoteActionMessageListHandler.convertProgressBar((ProgressBar) object));
            else
                result.add(object);
        }
        return new ListResult((ArrayList) result);
    }
}