package lsfusion.gwt.form.server.navigator.handlers;

import lsfusion.gwt.form.server.navigator.NavigatorActionHandler;
import lsfusion.gwt.base.shared.actions.BooleanResult;
import lsfusion.gwt.form.server.spring.LSFusionDispatchServlet;
import lsfusion.gwt.form.shared.actions.navigator.IsConfigurationAccessAllowedAction;
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
