package skolkovo.gwt.expertprofile.server;

import org.apache.log4j.Logger;
import org.springframework.security.core.Authentication;
import platform.base.DebugUtils;
import platform.gwt.base.server.LogicsServiceServlet;
import platform.gwt.base.shared.MessageException;
import skolkovo.api.gwt.shared.ProfileInfo;
import skolkovo.api.remote.SkolkovoRemoteInterface;
import skolkovo.gwt.expertprofile.client.ExpertProfileService;

import java.rmi.RemoteException;

public class ExpertProfileServiceImpl extends LogicsServiceServlet<SkolkovoRemoteInterface> implements ExpertProfileService {
    protected final static Logger logger = Logger.getLogger(ExpertProfileServiceImpl.class);

    @Override
    public ProfileInfo getProfileInfo() throws MessageException {
        try {
            Authentication auth = getAuthentication();
            if (auth == null) {
                return null;
            }

            return logics.getProfileInfo(auth.getName());
        } catch (RemoteException e) {
            logger.error("Ошибка в getProfileInfo: ", e);
            e.printStackTrace();
            throw new MessageException(DebugUtils.getInitialCause(e).getMessage());
        }
    }

    @Override
    public void sentVoteDocuments(int voteId) throws MessageException {
        try {
            Authentication auth = getAuthentication();
            if (auth == null) {
                return;
            }

            logics.sentVoteDocuments(auth.getName(), voteId);
        } catch (RemoteException e) {
            logger.error("Ошибка в sentVoteDocuments: ", e);
            e.printStackTrace();
            throw new MessageException(DebugUtils.getInitialCause(e).getMessage());
        }
    }
}