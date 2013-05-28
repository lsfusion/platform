package platform.gwt.paas.server.handlers;

import com.octo.captcha.service.CaptchaServiceException;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import org.springframework.web.context.request.RequestContextHolder;
import paas.api.remote.PaasRemoteInterface;
import platform.gwt.base.server.ServerUtils;
import platform.gwt.base.server.captcha.CaptchaServiceSingleton;
import platform.gwt.base.server.dispatch.SimpleActionHandlerEx;
import platform.gwt.paas.server.spring.PaasDispatchServlet;
import platform.gwt.paas.shared.actions.AddUserAction;
import platform.gwt.paas.shared.actions.AddUserResult;

import java.rmi.RemoteException;

public class AddUserHandler extends SimpleActionHandlerEx<AddUserAction, AddUserResult, PaasRemoteInterface> {
    public AddUserHandler(PaasDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public AddUserResult executeEx(final AddUserAction action, final ExecutionContext context) throws DispatchException, RemoteException {
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
        return new AddUserResult(servlet.getLogics().addUser(action.username, action.email, action.password, action.firstName, action.lastName, ServerUtils.getLocaleLanguage()));
    }
}
