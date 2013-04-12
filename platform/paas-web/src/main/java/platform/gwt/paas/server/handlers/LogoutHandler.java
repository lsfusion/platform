package platform.gwt.paas.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import org.springframework.beans.factory.annotation.Autowired;
import paas.api.remote.PaasRemoteInterface;
import platform.gwt.base.server.dispatch.SecuredActionHandler;
import platform.gwt.base.shared.actions.VoidResult;
import platform.gwt.paas.server.spring.PaasDispatchServlet;
import platform.gwt.paas.shared.actions.LogoutAction;

import javax.servlet.http.HttpSession;
import java.rmi.RemoteException;

public class LogoutHandler extends SecuredActionHandler<LogoutAction, VoidResult, PaasRemoteInterface> {
    @Autowired
    private HttpSession httpSession;

    public LogoutHandler(PaasDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(final LogoutAction action, final ExecutionContext context) throws DispatchException, RemoteException {
        if (httpSession != null) {
            httpSession.invalidate();
        }

        return new VoidResult();
    }
}

