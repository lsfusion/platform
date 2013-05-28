package platform.gwt.paas.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import paas.api.remote.PaasRemoteInterface;
import platform.gwt.base.server.dispatch.SecuredActionHandler;
import platform.gwt.paas.server.spring.PaasDispatchServlet;
import platform.gwt.paas.shared.actions.GetProjectsResult;
import platform.gwt.paas.shared.actions.RemoveProjectAction;

import java.rmi.RemoteException;

public class RemoveProjectHandler extends SecuredActionHandler<RemoveProjectAction, GetProjectsResult, PaasRemoteInterface> {

    public RemoveProjectHandler(PaasDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public GetProjectsResult executeEx(final RemoveProjectAction action, final ExecutionContext context) throws DispatchException, RemoteException {
        return new GetProjectsResult(
                servlet.getLogics().removeProject(
                        getAuthentication().getName(), action.projectId));
    }
}

