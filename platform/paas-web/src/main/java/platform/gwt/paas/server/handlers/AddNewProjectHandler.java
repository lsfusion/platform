package platform.gwt.paas.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import paas.api.remote.PaasRemoteInterface;
import platform.gwt.base.server.dispatch.SecuredActionHandler;
import platform.gwt.paas.server.spring.PaasDispatchServlet;
import platform.gwt.paas.shared.actions.AddNewProjectAction;
import platform.gwt.paas.shared.actions.GetProjectsResult;

import java.rmi.RemoteException;

public class AddNewProjectHandler extends SecuredActionHandler<AddNewProjectAction, GetProjectsResult, PaasRemoteInterface> {

    public AddNewProjectHandler(PaasDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public GetProjectsResult executeEx(final AddNewProjectAction action, final ExecutionContext context) throws DispatchException, RemoteException {
        return new GetProjectsResult(
                servlet.getLogics().addNewProject(
                        getAuthentication().getName(), action.project));
    }
}

