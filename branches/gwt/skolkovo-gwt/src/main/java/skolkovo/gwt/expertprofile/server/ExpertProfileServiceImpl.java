package skolkovo.gwt.expertprofile.server;

import org.apache.log4j.Logger;
import platform.base.DebugUtils;
import platform.gwt.base.server.LogicsServiceServlet;
import platform.gwt.base.shared.MessageException;
import skolkovo.api.gwt.shared.ProfileInfo;
import skolkovo.api.remote.SkolkovoRemoteInterface;
import skolkovo.gwt.expertprofile.client.ExpertProfileService;

import java.rmi.RemoteException;
import java.security.Principal;

public class ExpertProfileServiceImpl extends LogicsServiceServlet<SkolkovoRemoteInterface> implements ExpertProfileService {
    protected final static Logger logger = Logger.getLogger(ExpertProfileServiceImpl.class);

    @Override
    public ProfileInfo getProfileInfo() throws MessageException {
        try {
            Principal user = getThreadLocalRequest().getUserPrincipal();
            if (user == null) {
                return null;
            }

            return logics.getProfileInfo(user.getName());
        } catch (RemoteException e) {
            logger.error("Ошибка в getProfileInfo: ", e);
            e.printStackTrace();
            throw new MessageException(DebugUtils.getInitialCause(e).getMessage());
        }
    }

    @Override
    public void sentVoteDocuments(int voteId) throws MessageException {
        try {
            Principal user = getThreadLocalRequest().getUserPrincipal();
            if (user == null) {
                return;
            }

            logics.sentVoteDocuments(user.getName(), voteId);
        } catch (RemoteException e) {
            logger.error("Ошибка в sentVoteDocuments: ", e);
            e.printStackTrace();
            throw new MessageException(DebugUtils.getInitialCause(e).getMessage());
        }
    }
}