package platform.gwt.paas.server.handlers;

import com.gwtplatform.dispatch.server.ExecutionContext;
import com.gwtplatform.dispatch.shared.ActionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import paas.api.remote.PaasRemoteInterface;
import platform.gwt.base.server.spring.BusinessLogicsProvider;
import platform.gwt.paas.shared.actions.GetAvailableModulesAction;
import platform.gwt.paas.shared.actions.GetModulesResult;

import java.rmi.RemoteException;

@Component
public class GetAvailableModulesHandler extends SimpleActionHandlerEx<GetAvailableModulesAction, GetModulesResult> {

    @Autowired
    private BusinessLogicsProvider<PaasRemoteInterface> blProvider;

    @Override
    public GetModulesResult executeEx(final GetAvailableModulesAction action, final ExecutionContext context) throws ActionException, RemoteException {
        return new GetModulesResult(
                blProvider.getLogics().getAvailalbeModules(
                        getAuthentication().getName(), action.projectId));
    }
}

