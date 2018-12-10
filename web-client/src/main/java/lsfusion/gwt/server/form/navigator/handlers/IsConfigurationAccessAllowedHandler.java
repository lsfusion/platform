package lsfusion.gwt.server.form.navigator.handlers;

import lsfusion.gwt.server.form.navigator.NavigatorActionHandler;
import lsfusion.gwt.shared.base.actions.BooleanResult;
import lsfusion.gwt.server.form.LSFusionDispatchServlet;
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
