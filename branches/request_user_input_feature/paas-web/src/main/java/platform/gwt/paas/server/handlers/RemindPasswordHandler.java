package platform.gwt.paas.server.handlers;

import com.gwtplatform.dispatch.server.ExecutionContext;
import com.gwtplatform.dispatch.shared.ActionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import paas.api.remote.PaasRemoteInterface;
import platform.gwt.base.server.ServerUtils;
import platform.gwt.base.server.spring.BusinessLogicsProvider;
import platform.gwt.paas.shared.actions.RemindPasswordAction;
import platform.gwt.paas.shared.actions.VoidResult;

import java.io.IOException;

@Component
public class RemindPasswordHandler extends SimpleActionHandlerEx<RemindPasswordAction, VoidResult> {
    @Autowired
    private BusinessLogicsProvider<PaasRemoteInterface> blProvider;

    public RemindPasswordHandler() {
        super(RemindPasswordAction.class);
    }

    @Override
    public VoidResult executeEx(RemindPasswordAction action, ExecutionContext context) throws ActionException, IOException {
        blProvider.getLogics().remindPassword(action.email, ServerUtils.getLocaleLanguage());
        return new VoidResult();
    }
}
