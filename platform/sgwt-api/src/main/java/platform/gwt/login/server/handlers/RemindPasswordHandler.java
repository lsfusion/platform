package platform.gwt.login.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.base.server.ServerUtils;
import platform.gwt.base.server.dispatch.SimpleActionHandlerEx;
import platform.gwt.base.shared.actions.VoidResult;
import platform.gwt.login.server.LoginDispatchServlet;
import platform.gwt.login.shared.actions.RemindPassword;
import platform.interop.RemoteLogicsInterface;

import java.io.IOException;

public class RemindPasswordHandler extends SimpleActionHandlerEx<RemindPassword, VoidResult, RemoteLogicsInterface> {
    public RemindPasswordHandler(LoginDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(RemindPassword action, ExecutionContext context) throws DispatchException, IOException {
        servlet.getLogics().remindPassword(action.email, ServerUtils.getLocaleLanguage());
        return new VoidResult();
    }
}
