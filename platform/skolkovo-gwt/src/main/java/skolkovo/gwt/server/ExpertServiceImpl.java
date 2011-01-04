package skolkovo.gwt.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import skolkovo.api.remote.SkolkovoRemoteInterface;
import skolkovo.gwt.client.ExpertService;
import skolkovo.gwt.shared.GwtVoteInfo;

import java.rmi.RemoteException;
import java.security.Principal;

public class ExpertServiceImpl extends RemoteServiceServlet implements ExpertService {
    public GwtVoteInfo getVoteInfo(int voteId) {
        try {
            Principal user = getThreadLocalRequest().getUserPrincipal();
            if (user == null) {
                return null;
            }

            SkolkovoRemoteInterface logics = SkolkovoLogicsClient.getInstance().getLogics();
            return VoteFactory.toGwtVoteInfo(logics.getVoteInfo(user.getName(), voteId));
        } catch (RemoteException e) {
            System.err.println("Exception while getting vote info.");
            e.printStackTrace();
        }

        return null;
    }

    public void setVoteInfo(GwtVoteInfo voteInfo, int voteId) {
        try {
            Principal user = getThreadLocalRequest().getUserPrincipal();
            if (user != null) {
                SkolkovoRemoteInterface logics = SkolkovoLogicsClient.getInstance().getLogics();
                logics.setVoteInfo(user.getName(), voteId, VoteFactory.toVoteInfo(voteInfo));
            }
        } catch (RemoteException e) {
            System.err.println("Exception while setting vote info.");
            e.printStackTrace();
        }
    }
}