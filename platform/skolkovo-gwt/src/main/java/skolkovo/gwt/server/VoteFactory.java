package skolkovo.gwt.server;

import skolkovo.VoteInfo;
import skolkovo.gwt.shared.GwtVoteInfo;

public class VoteFactory {
    public static GwtVoteInfo toGwtVoteInfo(VoteInfo voteInfo) {
        GwtVoteInfo gwtVoteInfo = new GwtVoteInfo();
        gwtVoteInfo.expertName = voteInfo.expertName;
        return gwtVoteInfo;
    }

    public static VoteInfo toVoteInfo(GwtVoteInfo gwtVoteInfo) {
        VoteInfo voteInfo = new VoteInfo();
        return voteInfo;
    }
}
