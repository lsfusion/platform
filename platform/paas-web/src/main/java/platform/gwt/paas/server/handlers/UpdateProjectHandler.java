package platform.gwt.paas.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import paas.api.remote.PaasRemoteInterface;
import platform.gwt.base.server.dispatch.SecuredActionHandler;
import platform.gwt.paas.server.spring.PaasDispatchServlet;
import platform.gwt.paas.shared.actions.GetProjectsResult;
import platform.gwt.paas.shared.actions.UpdateProjectAction;

import java.rmi.RemoteException;

public class UpdateProjectHandler extends SecuredActionHandler<UpdateProjectAction, GetProjectsResult, PaasRemoteInterface> {

    public UpdateProjectHandler(PaasDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public GetProjectsResult executeEx(final UpdateProjectAction action, final ExecutionContext context) throws DispatchException, RemoteException {
        return new GetProjectsResult(
                servlet.getLogics().updateProject(
                        getAuthentication().getName(), action.project));
    }
}

