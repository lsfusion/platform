package platform.gwt.paas.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import paas.api.remote.PaasRemoteInterface;
import platform.gwt.base.server.dispatch.SecuredActionHandler;
import platform.gwt.paas.server.spring.PaasDispatchServlet;
import platform.gwt.paas.shared.actions.AddModulesAction;
import platform.gwt.paas.shared.actions.GetModulesResult;

import java.rmi.RemoteException;

public class AddModulesHandler extends SecuredActionHandler<AddModulesAction, GetModulesResult, PaasRemoteInterface> {

    public AddModulesHandler(PaasDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public GetModulesResult executeEx(final AddModulesAction action, final ExecutionContext context) throws DispatchException, RemoteException {
        return new GetModulesResult(
                servlet.getLogics().addModules(
                        getAuthentication().getName(), action.projectId, action.moduleIds));
    }
}

