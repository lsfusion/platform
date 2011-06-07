package skolkovo.gwt.expert.server;

import org.apache.log4j.Logger;
import platform.base.DebugUtils;
import skolkovo.api.serialization.VoteInfo;
import skolkovo.gwt.base.server.SkolkovoRemoteServiceServlet;
import skolkovo.gwt.base.shared.MessageException;
import skolkovo.gwt.expert.client.ExpertService;

public class ExpertServiceImpl extends SkolkovoRemoteServiceServlet implements ExpertService {
    protected final static Logger logger = Logger.getLogger(ExpertServiceImpl.class);

    public VoteInfo getVoteInfo(String voteId) throws MessageException {
        try {
//            Principal user = getThreadLocalRequest().getUserPrincipal();
//            if (user == null) {
//                return null;
//            }

            return skolkovo.getVoteInfo(voteId);
        } catch (Throwable e) {
            logger.error("Ошибка в getVoteInfo: ", e);
            e.printStackTrace();
            throw new MessageException(DebugUtils.getInitialCause(e).getMessage());
        }
    }

    public void setVoteInfo(VoteInfo voteInfo, String voteId) throws MessageException {
        try {
//            Principal user = getThreadLocalRequest().getUserPrincipal();
//            if (user != null) {
            skolkovo.setVoteInfo(voteId, voteInfo);
//            }
        } catch (Throwable e) {
            logger.error("Ошибка в setVoteInfo: ", e);
            e.printStackTrace();
            throw new MessageException(DebugUtils.getInitialCause(e).getMessage());
        }
    }
}