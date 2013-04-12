package skolkovo.gwt.claimer.server;

import net.customware.gwt.dispatch.server.InstanceActionHandlerRegistry;
import platform.gwt.base.server.LogicsAwareDispatchServlet;
import skolkovo.api.remote.SkolkovoRemoteInterface;

public class ClaimerDispatchServlet extends LogicsAwareDispatchServlet<SkolkovoRemoteInterface> {
    @Override
    protected void addHandlers(InstanceActionHandlerRegistry registry) {
    }
}