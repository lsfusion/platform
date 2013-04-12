package platform.gwt.paas.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import paas.api.remote.PaasRemoteInterface;
import platform.gwt.base.server.dispatch.SecuredActionHandler;
import platform.gwt.paas.server.spring.PaasDispatchServlet;
import platform.gwt.paas.shared.actions.GetModulesResult;
import platform.gwt.paas.shared.actions.RemoveModuleFromProjectAction;

import java.rmi.RemoteException;

public class RemoveModuleFromProjectHandler extends SecuredActionHandler<RemoveModuleFromProjectAction, GetModulesResult, PaasRemoteInterface> {

    public RemoveModuleFromProjectHandler(PaasDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public GetModulesResult executeEx(final RemoveModuleFromProjectAction action, final ExecutionContext context) throws DispatchException, RemoteException {
        return new GetModulesResult(
                servlet.getLogics().removeModuleFromProject(
                        getAuthentication().getName(), action.projectId, action.moduleId));
    }
}

