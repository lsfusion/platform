package platform.gwt.paas.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import paas.api.gwt.shared.dto.ModuleDTO;
import paas.api.remote.PaasRemoteInterface;
import platform.gwt.base.server.dispatch.SecuredActionHandler;
import platform.gwt.paas.server.spring.PaasDispatchServlet;
import platform.gwt.paas.shared.actions.AddNewModuleAction;
import platform.gwt.paas.shared.actions.GetModulesResult;

import java.rmi.RemoteException;

public class AddNewModuleHandler extends SecuredActionHandler<AddNewModuleAction, GetModulesResult, PaasRemoteInterface> {

    public AddNewModuleHandler(PaasDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public GetModulesResult executeEx(final AddNewModuleAction action, final ExecutionContext context) throws DispatchException, RemoteException {
        ModuleDTO newModule = new ModuleDTO();
        newModule.name = action.moduleName;

        return new GetModulesResult(
                servlet.getLogics().addNewModule(
                        getAuthentication().getName(), action.projectId, newModule));
    }
}

