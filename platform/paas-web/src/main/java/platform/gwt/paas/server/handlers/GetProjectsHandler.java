package platform.gwt.paas.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import paas.api.remote.PaasRemoteInterface;
import platform.gwt.base.server.dispatch.SecuredActionHandler;
import platform.gwt.paas.server.spring.PaasDispatchServlet;
import platform.gwt.paas.shared.actions.GetProjectsAction;
import platform.gwt.paas.shared.actions.GetProjectsResult;

import java.rmi.RemoteException;

public class GetProjectsHandler extends SecuredActionHandler<GetProjectsAction, GetProjectsResult, PaasRemoteInterface> {

    public GetProjectsHandler(PaasDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public GetProjectsResult executeEx(final GetProjectsAction action, final ExecutionContext context) throws DispatchException, RemoteException {
        return new GetProjectsResult(
                servlet.getLogics().getProjects(
                        getAuthentication().getName()));
    }
}

