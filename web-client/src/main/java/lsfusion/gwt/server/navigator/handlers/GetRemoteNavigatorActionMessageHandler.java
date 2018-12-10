package lsfusion.gwt.server.navigator.handlers;

import lsfusion.gwt.server.LSFusionDispatchServlet;
import lsfusion.gwt.server.navigator.NavigatorActionHandler;
import lsfusion.gwt.shared.form.actions.navigator.GetRemoteNavigatorActionMessage;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import net.customware.gwt.dispatch.shared.general.StringResult;

import java.io.IOException;

public class GetRemoteNavigatorActionMessageHandler extends NavigatorActionHandler<GetRemoteNavigatorActionMessage, StringResult> {

    public GetRemoteNavigatorActionMessageHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    protected String getActionDetails(GetRemoteNavigatorActionMessage action) {
        return null; // too many logs
    }

    @Override
    public StringResult executeEx(GetRemoteNavigatorActionMessage action, ExecutionContext context) throws DispatchException, IOException {
        return new StringResult(getRemoteNavigator(action).getRemoteActionMessage());
    }
}
