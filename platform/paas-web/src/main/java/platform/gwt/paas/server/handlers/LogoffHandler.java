package platform.gwt.paas.server.handlers;

import com.gwtplatform.dispatch.server.ExecutionContext;
import com.gwtplatform.dispatch.shared.ActionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import platform.gwt.paas.shared.actions.LogoffAction;
import platform.gwt.paas.shared.actions.VoidResult;

import javax.servlet.http.HttpSession;
import java.rmi.RemoteException;

@Component
public class LogoffHandler extends SimpleActionHandlerEx<LogoffAction, VoidResult> {
    @Autowired
    private HttpSession httpSession;

    public LogoffHandler() {
        super(LogoffAction.class);
    }

    @Override
    public VoidResult executeEx(final LogoffAction action, final ExecutionContext context) throws ActionException, RemoteException {
        if (httpSession != null) {
            httpSession.invalidate();
        }

        return new VoidResult();
    }
}

