package platform.gwt.paas.server.handlers;

import com.gwtplatform.dispatch.server.ExecutionContext;
import com.gwtplatform.dispatch.shared.ActionException;
import com.octo.captcha.service.CaptchaServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import paas.api.remote.PaasRemoteInterface;
import platform.gwt.base.server.ServerUtils;
import platform.gwt.base.server.captcha.CaptchaServiceSingleton;
import platform.gwt.base.server.spring.BusinessLogicsProvider;
import platform.gwt.paas.shared.actions.AddUserResult;
import platform.gwt.paas.shared.actions.AddUserAction;

import java.rmi.RemoteException;

@Component
public class AddUserHandler extends SimpleActionHandlerEx<AddUserAction, AddUserResult> {
    @Autowired
    private BusinessLogicsProvider<PaasRemoteInterface> blProvider;

    public AddUserHandler() {
        super(AddUserAction.class);
    }

    @Override
    public AddUserResult executeEx(final AddUserAction action, final ExecutionContext context) throws ActionException, RemoteException {
        String captchaResponse = action.captchaText.toUpperCase();
        boolean isResponseCorrect;
        String captchaId = RequestContextHolder.currentRequestAttributes().getSessionId() + action.captchaSalt;
        try {
            isResponseCorrect = CaptchaServiceSingleton.getInstance().validateResponseForID(captchaId, captchaResponse);
        } catch (CaptchaServiceException e) {
            isResponseCorrect = false;
        }
        if (!isResponseCorrect) {
            return new AddUserResult("wrongCaptcha");
        }
        return new AddUserResult(blProvider.getLogics().addUser(action.username, action.email, action.password, action.firstName, action.lastName, ServerUtils.getLocaleLanguage()));
    }
}
