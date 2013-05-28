package skolkovo.gwt.expertprofile.server;

import net.customware.gwt.dispatch.server.InstanceActionHandlerRegistry;
import platform.gwt.base.server.LogicsAwareDispatchServlet;
import skolkovo.api.remote.SkolkovoRemoteInterface;
import skolkovo.gwt.expertprofile.server.handlers.GetProfileInfoHandler;
import skolkovo.gwt.expertprofile.server.handlers.SentVoteDocumentsHandler;
import skolkovo.gwt.expertprofile.server.handlers.SetProfileInfoHandler;

public class ExpertProfileDispatchServlet extends LogicsAwareDispatchServlet<SkolkovoRemoteInterface> {
    @Override
    protected void addHandlers(InstanceActionHandlerRegistry registry) {
        registry.addHandler(new GetProfileInfoHandler(this));
        registry.addHandler(new SentVoteDocumentsHandler(this));
        registry.addHandler(new SetProfileInfoHandler(this));
    }
}