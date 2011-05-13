package skolkovo.gwt.base.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import skolkovo.api.remote.SkolkovoRemoteInterface;
import skolkovo.gwt.expert.server.SkolkovoLogicsClient;

public abstract class SkolkovoRemoteServiceServlet extends RemoteServiceServlet {
    protected SkolkovoRemoteInterface getLogics() {
        return SkolkovoLogicsClient.getInstance().getLogics(getServletContext());
    }
}
