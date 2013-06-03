package lsfusion.gwt.login.server.handlers;

import com.octo.captcha.service.CaptchaServiceException;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import net.customware.gwt.dispatch.shared.general.StringResult;
import lsfusion.gwt.base.server.ServerUtils;
import lsfusion.gwt.base.server.captcha.CaptchaServiceSingleton;
import lsfusion.gwt.base.server.dispatch.SimpleActionHandlerEx;
import lsfusion.gwt.login.server.LoginDispatchServlet;
import lsfusion.gwt.login.shared.actions.AddUser;
import lsfusion.interop.RemoteLogicsInterface;

import java.io.IOException;

public class AddUserHandler extends SimpleActionHandlerEx<AddUser, StringResult, RemoteLogicsInterface> {
    public AddUserHandler(LoginDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public StringResult executeEx(AddUser action, ExecutionContext context) throws DispatchException, IOException {
        String captchaResponse = action.captchaText.toUpperCase();
        boolean isResponseCorrect;
        String captchaId = servlet.getRequest().getSession().getId();
        try {
            isResponseCorrect = CaptchaServiceSingleton.getInstance().validateResponseForID(captchaId + action.captchaSalt, captchaResponse);
        } catch (CaptchaServiceException e) {
            isResponseCorrect = false;
        }
        if (!isResponseCorrect) {
            return new StringResult("wrongCaptcha");
        }
        return new StringResult(servlet.getLogics().addUser(action.username, action.email, action.password, action.firstName, action.lastName, ServerUtils.getLocaleLanguage()));
    }
}
