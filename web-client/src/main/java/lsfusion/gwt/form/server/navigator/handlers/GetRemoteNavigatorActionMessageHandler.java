package lsfusion.gwt.form.server.navigator.handlers;

import lsfusion.gwt.form.server.spring.LSFusionDispatchServlet;
import lsfusion.gwt.form.server.navigator.NavigatorActionHandler;
import lsfusion.gwt.form.shared.actions.navigator.GetRemoteNavigatorActionMessage;
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
        return new StringResult(servlet.getNavigator().getRemoteActionMessage());
    }
}
