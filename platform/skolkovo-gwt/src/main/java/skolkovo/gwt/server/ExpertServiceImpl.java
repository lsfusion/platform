package skolkovo.gwt.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import skolkovo.api.remote.SkolkovoRemoteInterface;
import skolkovo.gwt.client.ExpertService;
import skolkovo.gwt.shared.GwtVoteInfo;

import java.rmi.RemoteException;

public class ExpertServiceImpl extends RemoteServiceServlet implements ExpertService {
    public GwtVoteInfo getVoteInfo(int expertId, int projectId) {
        try {
            SkolkovoRemoteInterface logics = SkolkovoLogicsClient.getInstance().getLogics();
            return VoteFactory.toGwtVoteInfo(logics.getVoteInfo(expertId, projectId));
        } catch (RemoteException e) {
            System.err.println("Exception while getting vote info.");
        }

        return null;
    }
}