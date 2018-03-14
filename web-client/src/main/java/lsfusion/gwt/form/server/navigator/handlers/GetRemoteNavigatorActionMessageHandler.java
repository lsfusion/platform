package lsfusion.gwt.form.server.navigator.handlers;

import lsfusion.gwt.base.server.LogicsAwareDispatchServlet;
import lsfusion.gwt.form.server.FormSessionObject;
import lsfusion.gwt.form.server.form.handlers.FormActionHandler;
import lsfusion.gwt.form.server.form.handlers.LoggableActionHandler;
import lsfusion.gwt.form.shared.actions.form.GetRemoteActionMessage;
import lsfusion.gwt.form.shared.actions.navigator.GetRemoteNavigatorActionMessage;
import lsfusion.interop.RemoteLogicsInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import net.customware.gwt.dispatch.shared.general.StringResult;

import java.io.IOException;

public class GetRemoteNavigatorActionMessageHandler extends LoggableActionHandler<GetRemoteNavigatorActionMessage, StringResult, RemoteLogicsInterface> {

    public GetRemoteNavigatorActionMessageHandler(LogicsAwareDispatchServlet<RemoteLogicsInterface> servlet) {
        super(servlet);
    }

    @Override
    public StringResult executeEx(GetRemoteNavigatorActionMessage action, ExecutionContext context) throws DispatchException, IOException {
        return new StringResult(servlet.getNavigator().getRemoteActionMessage());
    }
}
