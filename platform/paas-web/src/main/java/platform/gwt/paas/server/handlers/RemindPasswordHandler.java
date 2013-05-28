package platform.gwt.paas.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import paas.api.remote.PaasRemoteInterface;
import platform.gwt.base.server.ServerUtils;
import platform.gwt.base.server.dispatch.SimpleActionHandlerEx;
import platform.gwt.base.shared.actions.VoidResult;
import platform.gwt.paas.server.spring.PaasDispatchServlet;
import platform.gwt.paas.shared.actions.RemindPasswordAction;

import java.io.IOException;

public class RemindPasswordHandler extends SimpleActionHandlerEx<RemindPasswordAction, VoidResult, PaasRemoteInterface> {
    public RemindPasswordHandler(PaasDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(RemindPasswordAction action, ExecutionContext context) throws DispatchException, IOException {
        servlet.getLogics().remindPassword(action.email, ServerUtils.getLocaleLanguage());
        return new VoidResult();
    }
}
