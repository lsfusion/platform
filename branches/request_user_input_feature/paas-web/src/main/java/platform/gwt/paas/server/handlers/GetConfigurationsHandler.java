package platform.gwt.paas.server.handlers;

import com.gwtplatform.dispatch.server.ExecutionContext;
import com.gwtplatform.dispatch.shared.ActionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import paas.api.remote.PaasRemoteInterface;
import platform.gwt.base.server.spring.BusinessLogicsProvider;
import platform.gwt.paas.shared.actions.GetConfigurationsAction;
import platform.gwt.paas.shared.actions.GetConfigurationsResult;

import java.rmi.RemoteException;

@Component
public class GetConfigurationsHandler extends SimpleActionHandlerEx<GetConfigurationsAction, GetConfigurationsResult> {

    @Autowired
    private BusinessLogicsProvider<PaasRemoteInterface> blProvider;

    @Override
    public GetConfigurationsResult executeEx(final GetConfigurationsAction action, final ExecutionContext context) throws ActionException, RemoteException {
        return new GetConfigurationsResult(
                blProvider.getLogics().getProjectConfigurations(
                        getAuthentication().getName(), action.projectId));
    }
}

