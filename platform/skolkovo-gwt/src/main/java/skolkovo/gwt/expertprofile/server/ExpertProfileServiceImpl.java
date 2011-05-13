package skolkovo.gwt.expertprofile.server;

import org.apache.log4j.Logger;
import skolkovo.gwt.base.server.ConvertFactory;
import skolkovo.gwt.base.server.SkolkovoRemoteServiceServlet;
import skolkovo.gwt.base.shared.GwtProfileInfo;
import skolkovo.gwt.base.shared.MessageException;
import skolkovo.gwt.expert.server.DebugUtil;
import skolkovo.gwt.expertprofile.client.ExpertProfileService;

import java.rmi.RemoteException;
import java.security.Principal;

public class ExpertProfileServiceImpl extends SkolkovoRemoteServiceServlet implements ExpertProfileService {
    protected final static Logger logger = Logger.getLogger(ExpertProfileServiceImpl.class);

    @Override
    public GwtProfileInfo getProfileInfo() throws MessageException {
        try {
            Principal user = getThreadLocalRequest().getUserPrincipal();
            if (user == null) {
                return null;
            }

            return ConvertFactory.toGwtProfileInfo(getLogics().getProfileInfo(user.getName()));
        } catch (RemoteException e) {
            logger.error("Ошибка в getProfileInfo: ", e);
            e.printStackTrace();
            throw new MessageException(DebugUtil.getInitialCause(e).getMessage());
        }
    }

    @Override
    public void sentVoteDocuments(int voteId) throws MessageException {
        try {
            Principal user = getThreadLocalRequest().getUserPrincipal();
            if (user == null) {
                return;
            }

            getLogics().sentVoteDocuments(user.getName(), voteId);
        } catch (RemoteException e) {
            logger.error("Ошибка в sentVoteDocuments: ", e);
            e.printStackTrace();
            throw new MessageException(DebugUtil.getInitialCause(e).getMessage());
        }
    }
}