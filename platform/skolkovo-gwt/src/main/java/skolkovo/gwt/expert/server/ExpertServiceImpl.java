package skolkovo.gwt.expert.server;

import org.apache.log4j.Logger;
import skolkovo.gwt.base.server.ConvertFactory;
import skolkovo.gwt.base.server.SkolkovoRemoteServiceServlet;
import skolkovo.gwt.base.shared.GwtVoteInfo;
import skolkovo.gwt.base.shared.MessageException;
import skolkovo.gwt.expert.client.ExpertService;

public class ExpertServiceImpl extends SkolkovoRemoteServiceServlet implements ExpertService {
    protected final static Logger logger = Logger.getLogger(ExpertServiceImpl.class);

    public GwtVoteInfo getVoteInfo(String voteId) throws MessageException {
        try {
//            Principal user = getThreadLocalRequest().getUserPrincipal();
//            if (user == null) {
//                return null;
//            }

            return ConvertFactory.toGwtVoteInfo(getLogics().getVoteInfo(voteId));
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
            getLogics().setVoteInfo(voteId, ConvertFactory.toVoteInfo(voteInfo));
//            }
        } catch (Throwable e) {
            logger.error("Ошибка в setVoteInfo: ", e);
            e.printStackTrace();
            throw new MessageException(DebugUtil.getInitialCause(e).getMessage());
        }
    }
}