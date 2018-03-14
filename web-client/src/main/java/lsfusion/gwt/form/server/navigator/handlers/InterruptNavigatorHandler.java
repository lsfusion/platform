package lsfusion.gwt.form.server.navigator.handlers;

import lsfusion.gwt.base.server.LogicsAwareDispatchServlet;
import lsfusion.gwt.base.shared.actions.VoidResult;
import lsfusion.gwt.form.server.form.handlers.LoggableActionHandler;
import lsfusion.gwt.form.shared.actions.navigator.GetRemoteNavigatorActionMessage;
import lsfusion.gwt.form.shared.actions.navigator.InterruptNavigator;
import lsfusion.interop.RemoteLogicsInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import net.customware.gwt.dispatch.shared.general.StringResult;

import java.io.IOException;

public class InterruptNavigatorHandler extends LoggableActionHandler<InterruptNavigator, VoidResult, RemoteLogicsInterface> {

    public InterruptNavigatorHandler(LogicsAwareDispatchServlet<RemoteLogicsInterface> servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(InterruptNavigator action, ExecutionContext context) throws DispatchException, IOException {
        servlet.getNavigator().interrupt(action.cancelable);
        return new VoidResult();
    }
}