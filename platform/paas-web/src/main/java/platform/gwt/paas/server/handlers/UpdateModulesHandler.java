package platform.gwt.paas.server.handlers;

import com.gwtplatform.dispatch.server.ExecutionContext;
import com.gwtplatform.dispatch.shared.ActionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import paas.api.remote.PaasRemoteInterface;
import platform.gwt.base.server.spring.BusinessLogicsProvider;
import platform.gwt.paas.shared.actions.UpdateModulesAction;
import platform.gwt.paas.shared.actions.VoidResult;

import java.rmi.RemoteException;

@Component
public class UpdateModulesHandler extends SimpleActionHandlerEx<UpdateModulesAction, VoidResult> {

    @Autowired
    private BusinessLogicsProvider<PaasRemoteInterface> blProvider;

    public UpdateModulesHandler() {
        super(UpdateModulesAction.class);
    }

    @Override
    public VoidResult executeEx(final UpdateModulesAction action, final ExecutionContext context) throws ActionException, RemoteException {
        if (action.moduleIds == null
            || action.moduleTexts == null
            || action.moduleIds.length != action.moduleTexts.length) {
            throw new IllegalArgumentException("Wrong parameters");
        }

        blProvider.getLogics().updateModules(
                getAuthentication().getName(), action.moduleIds, action.moduleTexts);

        return new VoidResult();
    }
}

