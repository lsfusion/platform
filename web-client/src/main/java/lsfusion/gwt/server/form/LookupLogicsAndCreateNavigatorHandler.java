package lsfusion.gwt.server.form;

import com.google.common.base.Throwables;
import lsfusion.base.NavigatorInfo;
import lsfusion.base.SystemUtils;
import lsfusion.gwt.server.base.spring.LogicsRequestHandler;
import lsfusion.gwt.server.form.logics.LogicsConnection;
import lsfusion.gwt.server.form.logics.spring.LogicsHandlerProvider;
import lsfusion.gwt.server.form.navigator.spring.LogicsAndNavigatorProvider;
import lsfusion.gwt.server.form.navigator.spring.LogicsAndNavigatorSessionObject;
import lsfusion.gwt.server.form.spring.LSFusionDispatchServlet;
import lsfusion.gwt.shared.form.actions.LookupLogicsAndCreateNavigator;
import lsfusion.gwt.shared.form.actions.navigator.CloseNavigator;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import net.customware.gwt.dispatch.shared.general.StringResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Locale;

public class LookupLogicsAndCreateNavigatorHandler extends lsfusion.gwt.server.base.dispatch.SimpleActionHandlerEx<LookupLogicsAndCreateNavigator, StringResult, RemoteLogicsInterface> {

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
