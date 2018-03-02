package lsfusion.gwt.form.server.navigator.handlers;

import lsfusion.gwt.base.server.LogicsAwareDispatchServlet;
import lsfusion.gwt.base.server.dispatch.NavigatorActionHandler;
import lsfusion.gwt.base.shared.actions.VoidResult;
import lsfusion.gwt.form.server.form.handlers.LoggableActionHandler;
import lsfusion.gwt.form.shared.actions.navigator.RegisterTabAction;
import lsfusion.interop.RemoteLogicsInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class RegisterTabActionHandler extends LoggableActionHandler<RegisterTabAction, VoidResult, RemoteLogicsInterface> implements NavigatorActionHandler {
    public RegisterTabActionHandler(LogicsAwareDispatchServlet<RemoteLogicsInterface> servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(RegisterTabAction action, ExecutionContext context) throws DispatchException, IOException {
        servlet.tabOpened(action.tabSID);
        return new VoidResult();
    }
}
