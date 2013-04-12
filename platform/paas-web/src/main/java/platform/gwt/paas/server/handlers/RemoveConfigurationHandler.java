package platform.gwt.paas.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import paas.api.remote.PaasRemoteInterface;
import platform.gwt.base.server.dispatch.SecuredActionHandler;
import platform.gwt.paas.server.spring.PaasDispatchServlet;
import platform.gwt.paas.shared.actions.GetConfigurationsResult;
import platform.gwt.paas.shared.actions.RemoveConfigurationAction;

import java.rmi.RemoteException;

public class RemoveConfigurationHandler extends SecuredActionHandler<RemoveConfigurationAction, GetConfigurationsResult, PaasRemoteInterface> {

    public RemoveConfigurationHandler(PaasDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public GetConfigurationsResult executeEx(final RemoveConfigurationAction action, final ExecutionContext context) throws DispatchException, RemoteException {
        return new GetConfigurationsResult(
                servlet.getLogics().removeConfiguration(
                        getAuthentication().getName(), action.projectId, action.configurationId));
    }
}

