package skolkovo.gwt.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import org.apache.log4j.Logger;
import skolkovo.api.remote.SkolkovoRemoteInterface;
import skolkovo.gwt.client.ExpertService;
import skolkovo.gwt.shared.GwtVoteInfo;
import skolkovo.gwt.shared.MessageException;

public class ExpertServiceImpl extends RemoteServiceServlet implements ExpertService {
    protected final static Logger logger = Logger.getLogger(RemoteServiceServlet.class);

    public GwtVoteInfo getVoteInfo(String voteId) throws MessageException {
        try {
//            Principal user = getThreadLocalRequest().getUserPrincipal();
//            if (user == null) {
//                return null;
//            }

            return VoteFactory.toGwtVoteInfo(getLogics().getVoteInfo(voteId));
        } catch (Throwable e) {
            logger.error("Ошибка в getVoteInfo: ", e);
            e.printStackTrace();
            throw new MessageException(DebugUtil.getInitialCause(e).getMessage());
        }
    }

    public void setVoteInfo(GwtVoteInfo voteInfo, String voteId) throws MessageException {
        try {
//            Principal user = getThreadLocalRequest().getUserPrincipal();
//            if (user != null) {
            getLogics().setVoteInfo(voteId, VoteFactory.toVoteInfo(voteInfo));
//            }
        } catch (Throwable e) {
            logger.error("Ошибка в setVoteInfo: ", e);
            e.printStackTrace();
            throw new MessageException(DebugUtil.getInitialCause(e).getMessage());
        }
    }

    private SkolkovoRemoteInterface getLogics() {
        String serverHost = getServletConfig().getInitParameter("serverHost");
        String serverPort = getServletConfig().getInitParameter("serverPort");
        return SkolkovoLogicsClient.getInstance().getLogics(serverHost, serverPort);
    }
}