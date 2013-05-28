package platform.gwt.paas.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import paas.api.remote.PaasRemoteInterface;
import platform.gwt.base.server.dispatch.SecuredActionHandler;
import platform.gwt.paas.server.spring.PaasDispatchServlet;
import platform.gwt.paas.shared.actions.GetModuleTextAction;
import platform.gwt.paas.shared.actions.GetModuleTextResult;

import java.rmi.RemoteException;

public class GetModuleTextHandler extends SecuredActionHandler<GetModuleTextAction, GetModuleTextResult, PaasRemoteInterface> {

    public GetModuleTextHandler(PaasDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public GetModuleTextResult executeEx(final GetModuleTextAction action, final ExecutionContext context) throws DispatchException, RemoteException {
        return new GetModuleTextResult(
                servlet.getLogics().getModuleText(
                        getAuthentication().getName(), action.moduleId));
    }
}

