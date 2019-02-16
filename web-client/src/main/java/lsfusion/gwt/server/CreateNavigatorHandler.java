package lsfusion.gwt.server;

import lsfusion.gwt.server.logics.LogicsActionHandler;
import lsfusion.gwt.shared.actions.CreateNavigatorAction;
import lsfusion.http.provider.logics.LogicsRunnable;
import lsfusion.http.provider.logics.LogicsSessionObject;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import net.customware.gwt.dispatch.shared.general.StringResult;

import java.io.IOException;

public class CreateNavigatorHandler extends LogicsActionHandler<CreateNavigatorAction, StringResult> {

    public CreateNavigatorHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public StringResult executeEx(final CreateNavigatorAction action, ExecutionContext context) throws DispatchException, IOException {
         return runRequest(action, new LogicsRunnable<StringResult>() {
            public StringResult run(LogicsSessionObject sessionObject) throws IOException {
                return new StringResult(servlet.getNavigatorProvider().createNavigator(sessionObject, servlet.getRequest()));
            }
        });
    }

    protected String getActionDetails(CreateNavigatorAction action) {
        return super.getActionDetails(action) + " TAB IN " + servlet.getSessionInfo();
    }

}
