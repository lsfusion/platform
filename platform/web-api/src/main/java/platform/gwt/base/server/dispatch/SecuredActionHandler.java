package platform.gwt.base.server.dispatch;

import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.Result;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import platform.gwt.base.server.LogicsAwareDispatchServlet;
import platform.interop.RemoteLogicsInterface;

public abstract class SecuredActionHandler<A extends Action<R>, R extends Result, L extends RemoteLogicsInterface> extends SimpleActionHandlerEx<A, R, L> implements SecuredAction<R> {
    public SecuredActionHandler(LogicsAwareDispatchServlet<L> servlet) {
        super(servlet);
    }

    @Override
    public boolean isAllowed() {
//        return true;
        return getAuthentication() != null;
    }

    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
