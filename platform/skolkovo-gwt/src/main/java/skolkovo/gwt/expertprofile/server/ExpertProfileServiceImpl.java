package skolkovo.gwt.expertprofile.server;

import net.customware.gwt.dispatch.server.InstanceActionHandlerRegistry;
import platform.gwt.base.server.LogicsDispatchServlet;
import skolkovo.api.remote.SkolkovoRemoteInterface;
import skolkovo.gwt.expertprofile.server.handlers.GetProfileInfoHandler;
import skolkovo.gwt.expertprofile.server.handlers.SentVoteDocumentsHandler;

public class ExpertProfileServiceImpl extends LogicsDispatchServlet<SkolkovoRemoteInterface> {
    @Override
    protected void addHandlers(InstanceActionHandlerRegistry registry) {
        registry.addHandler(new GetProfileInfoHandler(this));
        registry.addHandler(new SentVoteDocumentsHandler(this));
    }
}