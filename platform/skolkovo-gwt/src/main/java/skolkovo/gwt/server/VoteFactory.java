package skolkovo.gwt.server;

import skolkovo.VoteInfo;
import skolkovo.gwt.shared.GwtVoteInfo;

public class VoteFactory {
    public static GwtVoteInfo toGwtVoteInfo(VoteInfo voteInfo) {
        //todo:
        GwtVoteInfo gwtVoteInfo = new GwtVoteInfo();
        gwtVoteInfo.expertName = voteInfo.expertName;
        return gwtVoteInfo;
    }

    public static VoteInfo toVoteInfo(GwtVoteInfo gwtVoteInfo) {
        //todo:
        VoteInfo voteInfo = new VoteInfo();
        return voteInfo;
    }
}
