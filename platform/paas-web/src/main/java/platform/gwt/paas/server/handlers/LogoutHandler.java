package platform.gwt.paas.server.handlers;

import com.gwtplatform.dispatch.server.ExecutionContext;
import com.gwtplatform.dispatch.shared.ActionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import platform.gwt.paas.shared.actions.LogoutAction;
import platform.gwt.paas.shared.actions.VoidResult;

import javax.servlet.http.HttpSession;
import java.rmi.RemoteException;

@Component
public class LogoutHandler extends SimpleActionHandlerEx<LogoutAction, VoidResult> {
    @Autowired
    private HttpSession httpSession;

    public LogoutHandler() {
        super(LogoutAction.class);
    }

    @Override
    public VoidResult executeEx(final LogoutAction action, final ExecutionContext context) throws ActionException, RemoteException {
        if (httpSession != null) {
            httpSession.invalidate();
        }

        return new VoidResult();
    }
}

