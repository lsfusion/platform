package lsfusion.gwt.server.navigator.handlers;

import lsfusion.base.ProgressBar;
import lsfusion.gwt.shared.result.ListResult;
import lsfusion.gwt.server.LSFusionDispatchServlet;
import lsfusion.gwt.server.convert.ClientActionToGwtConverter;
import lsfusion.gwt.server.navigator.NavigatorActionHandler;
import lsfusion.gwt.shared.form.actions.navigator.GetRemoteNavigatorActionMessageList;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GetRemoteNavigatorActionMessageListHandler extends NavigatorActionHandler<GetRemoteNavigatorActionMessageList, ListResult> {
    private static ClientActionToGwtConverter clientActionConverter = ClientActionToGwtConverter.getInstance();

    public GetRemoteNavigatorActionMessageListHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    protected String getActionDetails(GetRemoteNavigatorActionMessageList action) {
        return null; // too many logs
    }

    @Override
    public ListResult executeEx(GetRemoteNavigatorActionMessageList action, ExecutionContext context) throws DispatchException, IOException {
        List<Object> result = new ArrayList<>();
        for (Object object : getRemoteNavigator(action).getRemoteActionMessageList()) {
            if (object instanceof ProgressBar)
                result.add(clientActionConverter.convertProgressBar((lsfusion.base.ProgressBar) object));
            else
                result.add(object);
        }
        return new ListResult((ArrayList) result);
    }
}