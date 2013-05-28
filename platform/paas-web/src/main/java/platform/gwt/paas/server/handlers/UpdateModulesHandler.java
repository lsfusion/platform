package platform.gwt.paas.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import paas.api.remote.PaasRemoteInterface;
import platform.gwt.base.server.dispatch.SecuredActionHandler;
import platform.gwt.base.shared.actions.VoidResult;
import platform.gwt.paas.server.spring.PaasDispatchServlet;
import platform.gwt.paas.shared.actions.UpdateModulesAction;

import java.rmi.RemoteException;

public class UpdateModulesHandler extends SecuredActionHandler<UpdateModulesAction, VoidResult, PaasRemoteInterface> {

    public UpdateModulesHandler(PaasDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(final UpdateModulesAction action, final ExecutionContext context) throws DispatchException, RemoteException {
        if (action.moduleIds == null
            || action.moduleTexts == null
            || action.moduleIds.length != action.moduleTexts.length) {
            throw new IllegalArgumentException("Wrong parameters");
        }

        servlet.getLogics().updateModules(
                getAuthentication().getName(), action.moduleIds, action.moduleTexts);

        return new VoidResult();
    }
}

