package platform.gwt.paas.server.handlers;

import com.gwtplatform.dispatch.server.ExecutionContext;
import com.gwtplatform.dispatch.shared.ActionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import paas.api.remote.PaasRemoteInterface;
import platform.gwt.base.server.spring.BusinessLogicsProvider;
import platform.gwt.paas.shared.actions.GetModulesAction;
import platform.gwt.paas.shared.actions.GetModulesResult;

import java.rmi.RemoteException;

@Component
public class GetModulesHandler extends SimpleActionHandlerEx<GetModulesAction, GetModulesResult> {

    @Autowired
    private BusinessLogicsProvider<PaasRemoteInterface> blProvider;

    @Override
    public GetModulesResult executeEx(final GetModulesAction action, final ExecutionContext context) throws ActionException, RemoteException {
        return new GetModulesResult(
                blProvider.getLogics().getProjectModules(
                        getAuthentication().getName(), action.projectId));
    }
}

