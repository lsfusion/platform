package platform.gwt.paas.server.handlers;

import com.gwtplatform.dispatch.server.ExecutionContext;
import com.gwtplatform.dispatch.shared.ActionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import paas.api.remote.PaasRemoteInterface;
import platform.gwt.base.server.spring.BusinessLogicsProvider;
import platform.gwt.paas.shared.actions.GetModuleTextAction;
import platform.gwt.paas.shared.actions.GetModuleTextResult;

import java.rmi.RemoteException;

@Component
public class GetModuleTextHandler extends SimpleActionHandlerEx<GetModuleTextAction, GetModuleTextResult> {

    @Autowired
    private BusinessLogicsProvider<PaasRemoteInterface> blProvider;

    public GetModuleTextHandler() {
        super(GetModuleTextAction.class);
    }

    @Override
    public GetModuleTextResult executeEx(final GetModuleTextAction action, final ExecutionContext context) throws ActionException, RemoteException {
        return new GetModuleTextResult(
                blProvider.getLogics().getModuleText(
                        getAuthentication().getName(), action.moduleId));
    }
}

