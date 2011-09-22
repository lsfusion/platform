package platform.gwt.paas.server.handlers;

import com.gwtplatform.dispatch.server.ExecutionContext;
import com.gwtplatform.dispatch.shared.ActionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import paas.api.remote.PaasRemoteInterface;
import platform.gwt.base.server.spring.BusinessLogicsProvider;
import platform.gwt.paas.shared.actions.GetModulesResult;
import platform.gwt.paas.shared.actions.RemoveModuleFromProjectAction;

import java.rmi.RemoteException;

@Component
public class RemoveModuleFromProjectHandler extends SimpleActionHandlerEx<RemoveModuleFromProjectAction, GetModulesResult> {

    @Autowired
    private BusinessLogicsProvider<PaasRemoteInterface> blProvider;

    public RemoveModuleFromProjectHandler() {
        super(RemoveModuleFromProjectAction.class);
    }

    @Override
    public GetModulesResult executeEx(final RemoveModuleFromProjectAction action, final ExecutionContext context) throws ActionException, RemoteException {
        return new GetModulesResult(
                blProvider.getLogics().removeModuleFromProject(
                        getAuthentication().getName(), action.projectId, action.moduleId));
    }
}

