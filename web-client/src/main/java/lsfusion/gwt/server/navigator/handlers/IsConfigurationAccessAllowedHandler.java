package lsfusion.gwt.server.navigator.handlers;

import lsfusion.gwt.server.navigator.NavigatorActionHandler;
import lsfusion.gwt.shared.result.BooleanResult;
import lsfusion.gwt.server.LSFusionDispatchServlet;
import lsfusion.gwt.shared.form.actions.navigator.IsConfigurationAccessAllowedAction;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class IsConfigurationAccessAllowedHandler extends NavigatorActionHandler<IsConfigurationAccessAllowedAction, BooleanResult> {
    public IsConfigurationAccessAllowedHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public BooleanResult executeEx(IsConfigurationAccessAllowedAction action, ExecutionContext context) throws DispatchException, IOException {
        return new BooleanResult(getRemoteNavigator(action).isConfigurationAccessAllowed());
    }
}
