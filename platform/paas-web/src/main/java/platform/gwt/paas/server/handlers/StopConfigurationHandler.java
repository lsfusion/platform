package platform.gwt.paas.server.handlers;

import com.gwtplatform.dispatch.server.ExecutionContext;
import com.gwtplatform.dispatch.shared.ActionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import paas.api.remote.PaasRemoteInterface;
import platform.gwt.base.server.spring.BusinessLogicsProvider;
import platform.gwt.paas.shared.actions.GetConfigurationsResult;
import platform.gwt.paas.shared.actions.StopConfigurationAction;

import java.rmi.RemoteException;

@Component
public class StopConfigurationHandler extends SimpleActionHandlerEx<StopConfigurationAction, GetConfigurationsResult> {

    @Autowired
    private BusinessLogicsProvider<PaasRemoteInterface> blProvider;

    public StopConfigurationHandler() {
        super(StopConfigurationAction.class);
    }

    @Override
    public GetConfigurationsResult executeEx(final StopConfigurationAction action, final ExecutionContext context) throws ActionException, RemoteException {
        return new GetConfigurationsResult(
                blProvider.getLogics().stopConfiguration(
                        getAuthentication().getName(), action.configurationId));
    }
}

