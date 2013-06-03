package lsfusion.gwt.login.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import lsfusion.gwt.base.server.ServerUtils;
import lsfusion.gwt.base.server.dispatch.SimpleActionHandlerEx;
import lsfusion.gwt.base.shared.actions.VoidResult;
import lsfusion.gwt.login.server.LoginDispatchServlet;
import lsfusion.gwt.login.shared.actions.RemindPassword;
import lsfusion.interop.RemoteLogicsInterface;

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
