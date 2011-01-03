package skolkovo.gwt.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import skolkovo.api.remote.SkolkovoRemoteInterface;
import skolkovo.gwt.client.ExpertService;

import java.rmi.RemoteException;

public class ExpertServiceImpl extends RemoteServiceServlet implements ExpertService {
    public String[] getProjects() {
        String[] result = null;
        try {
            SkolkovoRemoteInterface logics = SkolkovoLoigicsClient.getInstance().getLogics();
            if (logics != null) {
                result = logics.getProjectNames(-1);
            }
        } catch (RemoteException e) {
            System.err.println("Exception while getting project infos.");
        }

        return result;
    }
}