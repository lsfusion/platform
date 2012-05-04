package platform.gwt.paas.server.handlers;

import com.gwtplatform.dispatch.server.ExecutionContext;
import com.gwtplatform.dispatch.shared.ActionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import paas.api.gwt.shared.dto.ModuleDTO;
import paas.api.remote.PaasRemoteInterface;
import platform.gwt.base.server.spring.BusinessLogicsProvider;
import platform.gwt.paas.shared.actions.AddNewModuleAction;
import platform.gwt.paas.shared.actions.GetModulesResult;

import java.rmi.RemoteException;

@Component
public class AddNewModuleHandler extends SimpleActionHandlerEx<AddNewModuleAction, GetModulesResult> {

    @Autowired
    private BusinessLogicsProvider<PaasRemoteInterface> blProvider;

    @Override
    public GetModulesResult executeEx(final AddNewModuleAction action, final ExecutionContext context) throws ActionException, RemoteException {
        ModuleDTO newModule = new ModuleDTO();
        newModule.name = action.moduleName;

        return new GetModulesResult(
                blProvider.getLogics().addNewModule(
                        getAuthentication().getName(), action.projectId, newModule));
    }
}

