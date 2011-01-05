package skolkovo.gwt.server;

import skolkovo.VoteInfo;
import skolkovo.gwt.shared.GwtVoteInfo;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class VoteFactory {
    public static GwtVoteInfo toGwtVoteInfo(VoteInfo voteInfo) {
        GwtVoteInfo gwtVoteInfo = new GwtVoteInfo();

        gwtVoteInfo.expertName = voteInfo.expertName;
        gwtVoteInfo.projectClaimer = emptyIfNull(voteInfo.projectClaimer);
        gwtVoteInfo.projectName = emptyIfNull(voteInfo.projectName);
        gwtVoteInfo.projectCluster = emptyIfNull(voteInfo.projectCluster);
        gwtVoteInfo.voteResult = emptyIfNull(voteInfo.voteResult);
        gwtVoteInfo.voteDone = !gwtVoteInfo.voteResult.trim().isEmpty();
        gwtVoteInfo.inCluster = voteInfo.inCluster;
        gwtVoteInfo.innovative = voteInfo.innovative;
        gwtVoteInfo.innovativeComment = emptyIfNull(voteInfo.innovativeComment);
        gwtVoteInfo.foreign = voteInfo.foreign;
        gwtVoteInfo.competent = max(1, min(voteInfo.complete, 5));
        gwtVoteInfo.complete = max(1, min(voteInfo.complete, 5));
        gwtVoteInfo.completeComment = emptyIfNull(voteInfo.completeComment);

        return gwtVoteInfo;
    }

    private static String emptyIfNull(String s) {
        return s == null ? "" : s;
    }

    public static VoteInfo toVoteInfo(GwtVoteInfo gwtVoteInfo) {
        VoteInfo voteInfo = new VoteInfo();

        voteInfo.expertName = gwtVoteInfo.expertName;
        voteInfo.projectClaimer = gwtVoteInfo.projectClaimer;
        voteInfo.projectName = gwtVoteInfo.projectName;
        voteInfo.projectCluster = gwtVoteInfo.projectCluster;
        voteInfo.voteDone = gwtVoteInfo.voteDone;
        voteInfo.voteResult = gwtVoteInfo.voteResult;
        voteInfo.inCluster = gwtVoteInfo.inCluster;
        voteInfo.innovative = gwtVoteInfo.innovative;
        voteInfo.innovativeComment = gwtVoteInfo.innovativeComment;
        voteInfo.foreign = gwtVoteInfo.foreign;
        voteInfo.competent = max(1, min(gwtVoteInfo.complete, 5));
        voteInfo.complete = max(1, min(gwtVoteInfo.complete, 5));
        voteInfo.completeComment = gwtVoteInfo.completeComment;

        return voteInfo;
    }
}
