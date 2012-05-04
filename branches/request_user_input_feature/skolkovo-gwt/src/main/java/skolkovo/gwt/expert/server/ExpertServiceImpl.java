package skolkovo.gwt.expert.server;

import net.customware.gwt.dispatch.server.InstanceActionHandlerRegistry;
import platform.gwt.base.server.LogicsDispatchServlet;
import skolkovo.api.remote.SkolkovoRemoteInterface;
import skolkovo.gwt.expert.server.handlers.GetVoteInfoHandler;
import skolkovo.gwt.expert.server.handlers.SetVoteInfoHandler;

public class ExpertServiceImpl extends LogicsDispatchServlet<SkolkovoRemoteInterface> {
    @Override
    protected void addHandlers(InstanceActionHandlerRegistry registry) {
        registry.addHandler(new GetVoteInfoHandler(this));
        registry.addHandler(new SetVoteInfoHandler(this));
    }
}