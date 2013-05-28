package platform.gwt.paas.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import paas.api.remote.PaasRemoteInterface;
import platform.gwt.base.server.dispatch.SecuredActionHandler;
import platform.gwt.paas.server.spring.PaasDispatchServlet;
import platform.gwt.paas.shared.actions.GetAvailableModulesAction;
import platform.gwt.paas.shared.actions.GetModulesResult;

import java.rmi.RemoteException;

public class GetAvailableModulesHandler extends SecuredActionHandler<GetAvailableModulesAction, GetModulesResult, PaasRemoteInterface> {

    public GetAvailableModulesHandler(PaasDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public GetModulesResult executeEx(final GetAvailableModulesAction action, final ExecutionContext context) throws DispatchException, RemoteException {
        return new GetModulesResult(
                servlet.getLogics().getAvailalbeModules(
                        getAuthentication().getName(), action.projectId));
    }
}

