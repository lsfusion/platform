package lsfusion.gwt.form.server.navigator.handlers;

import lsfusion.base.ProgressBar;
import lsfusion.gwt.base.shared.actions.ListResult;
import lsfusion.gwt.form.server.spring.LSFusionDispatchServlet;
import lsfusion.gwt.form.server.convert.ClientActionToGwtConverter;
import lsfusion.gwt.form.server.navigator.NavigatorActionHandler;
import lsfusion.gwt.form.shared.actions.navigator.GetRemoteNavigatorActionMessageList;
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