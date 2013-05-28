package platform.gwt.paas.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import paas.api.remote.PaasRemoteInterface;
import platform.gwt.base.server.dispatch.SecuredActionHandler;
import platform.gwt.paas.server.spring.PaasDispatchServlet;
import platform.gwt.paas.shared.actions.GetConfigurationsAction;
import platform.gwt.paas.shared.actions.GetConfigurationsResult;

import java.rmi.RemoteException;

public class GetConfigurationsHandler extends SecuredActionHandler<GetConfigurationsAction, GetConfigurationsResult, PaasRemoteInterface> {

    public GetConfigurationsHandler(PaasDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public GetConfigurationsResult executeEx(final GetConfigurationsAction action, final ExecutionContext context) throws DispatchException, RemoteException {
        return new GetConfigurationsResult(
                servlet.getLogics().getProjectConfigurations(
                        getAuthentication().getName(), action.projectId));
    }
}

