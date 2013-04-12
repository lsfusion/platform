package platform.gwt.paas.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import paas.api.remote.PaasRemoteInterface;
import platform.gwt.base.server.dispatch.SecuredActionHandler;
import platform.gwt.paas.server.spring.PaasDispatchServlet;
import platform.gwt.paas.shared.actions.GetConfigurationsResult;
import platform.gwt.paas.shared.actions.StopConfigurationAction;

import java.rmi.RemoteException;

public class StopConfigurationHandler extends SecuredActionHandler<StopConfigurationAction, GetConfigurationsResult, PaasRemoteInterface> {

    public StopConfigurationHandler(PaasDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public GetConfigurationsResult executeEx(final StopConfigurationAction action, final ExecutionContext context) throws DispatchException, RemoteException {
        return new GetConfigurationsResult(
                servlet.getLogics().stopConfiguration(
                        getAuthentication().getName(), action.configurationId));
    }
}

