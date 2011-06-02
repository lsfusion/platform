package skolkovo.gwt.expert.server;

import org.apache.log4j.Logger;
import platform.gwt.base.server.DebugUtil;
import platform.gwt.base.server.LogicsServiceServlet;
import platform.gwt.base.shared.MessageException;
import skolkovo.api.gwt.shared.VoteInfo;
import skolkovo.api.remote.SkolkovoRemoteInterface;
import skolkovo.gwt.expert.client.ExpertService;

public class ExpertServiceImpl extends LogicsServiceServlet<SkolkovoRemoteInterface> implements ExpertService {
    protected final static Logger logger = Logger.getLogger(ExpertServiceImpl.class);

    public VoteInfo getVoteInfo(String voteId) throws MessageException {
        try {
//            Principal user = getThreadLocalRequest().getUserPrincipal();
//            if (user == null) {
//                return null;
//            }

            return logics.getVoteInfo(voteId);
        } catch (Throwable e) {
            logger.error("Ошибка в getVoteInfo: ", e);
            e.printStackTrace();
            throw new MessageException(DebugUtil.getInitialCause(e).getMessage());
        }
    }

    public void setVoteInfo(VoteInfo voteInfo, String voteId) throws MessageException {
        try {
//            Principal user = getThreadLocalRequest().getUserPrincipal();
//            if (user != null) {
            logics.setVoteInfo(voteId, voteInfo);
//            }
        } catch (Throwable e) {
            logger.error("Ошибка в setVoteInfo: ", e);
            e.printStackTrace();
            throw new MessageException(DebugUtil.getInitialCause(e).getMessage());
        }
    }
}