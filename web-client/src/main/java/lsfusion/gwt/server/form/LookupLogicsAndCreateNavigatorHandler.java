package lsfusion.gwt.server.form;

import lsfusion.http.LogicsRequestHandler;
import lsfusion.gwt.server.form.logics.LogicsConnection;
import lsfusion.gwt.shared.form.actions.LookupLogicsAndCreateNavigator;
import lsfusion.interop.RemoteLogicsInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import net.customware.gwt.dispatch.shared.general.StringResult;

import java.io.IOException;

public class LookupLogicsAndCreateNavigatorHandler extends SimpleActionHandlerEx<LookupLogicsAndCreateNavigator, StringResult, RemoteLogicsInterface> {

    public LookupLogicsAndCreateNavigatorHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public StringResult executeEx(LookupLogicsAndCreateNavigator action, ExecutionContext context) throws DispatchException, IOException {
         return new StringResult(LogicsRequestHandler.runRequest(servlet.getLogicsHandlerProvider(), action.host, action.port, action.exportName, new LogicsRequestHandler.Runnable<String>() {
            public String run(RemoteLogicsInterface remoteLogics, LogicsConnection logicsConnection) throws IOException {
                return servlet.getLogicsAndNavigatorProvider().createNavigator(remoteLogics);
            }
        }));
    }

    protected String getActionDetails(LookupLogicsAndCreateNavigator action) {
        return super.getActionDetails(action) + " TAB IN " + servlet.getSessionInfo();
    }

}
