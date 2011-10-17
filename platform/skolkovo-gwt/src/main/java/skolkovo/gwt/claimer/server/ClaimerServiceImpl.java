package skolkovo.gwt.claimer.server;

import net.customware.gwt.dispatch.server.InstanceActionHandlerRegistry;
import platform.gwt.base.server.LogicsDispatchServlet;
import skolkovo.api.remote.SkolkovoRemoteInterface;

public class ClaimerServiceImpl extends LogicsDispatchServlet<SkolkovoRemoteInterface> {
    @Override
    protected void addHandlers(InstanceActionHandlerRegistry registry) {
    }
}