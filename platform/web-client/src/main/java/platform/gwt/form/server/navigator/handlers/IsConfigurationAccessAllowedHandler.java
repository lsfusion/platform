package platform.gwt.form.server.navigator.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.base.server.LogicsAwareDispatchServlet;
import platform.gwt.base.server.dispatch.SimpleActionHandlerEx;
import platform.gwt.base.shared.actions.BooleanResult;
import platform.gwt.form.shared.actions.navigator.IsConfigurationAccessAllowedAction;
import platform.interop.RemoteLogicsInterface;

import java.io.IOException;

public class IsConfigurationAccessAllowedHandler extends SimpleActionHandlerEx<IsConfigurationAccessAllowedAction, BooleanResult, RemoteLogicsInterface> {
    public IsConfigurationAccessAllowedHandler(LogicsAwareDispatchServlet<RemoteLogicsInterface> servlet) {
        super(servlet);
    }

    @Override
    public BooleanResult executeEx(IsConfigurationAccessAllowedAction action, ExecutionContext context) throws DispatchException, IOException {
        return new BooleanResult(servlet.getNavigator().isConfigurationAccessAllowed());
    }
}
