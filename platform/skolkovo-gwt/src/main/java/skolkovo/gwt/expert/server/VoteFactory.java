package skolkovo.gwt.expert.server;

import skolkovo.VoteInfo;
import skolkovo.gwt.expert.shared.GwtVoteInfo;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class VoteFactory {
    public static GwtVoteInfo toGwtVoteInfo(VoteInfo voteInfo) {
        GwtVoteInfo gwtVoteInfo = new GwtVoteInfo();

        gwtVoteInfo.expertName = voteInfo.expertName;
        gwtVoteInfo.projectClaimer = emptyIfNullAndTrim(voteInfo.projectClaimer);
        gwtVoteInfo.projectName = emptyIfNullAndTrim(voteInfo.projectName);
        gwtVoteInfo.projectCluster = emptyIfNullAndTrim(voteInfo.projectCluster);
        gwtVoteInfo.voteResult = emptyIfNullAndTrim(voteInfo.voteResult);
        gwtVoteInfo.voteDone = !gwtVoteInfo.voteResult.trim().isEmpty();
        gwtVoteInfo.inCluster = voteInfo.inCluster;
        gwtVoteInfo.innovative = voteInfo.innovative;
        gwtVoteInfo.innovativeComment = emptyIfNullAndTrim(voteInfo.innovativeComment);
        gwtVoteInfo.foreign = voteInfo.foreign;
        gwtVoteInfo.competent = max(1, min(voteInfo.competent, 5));
        gwtVoteInfo.complete = max(1, min(voteInfo.complete, 5));
        gwtVoteInfo.completeComment = emptyIfNullAndTrim(voteInfo.completeComment);
        gwtVoteInfo.date = voteInfo.date;

        return gwtVoteInfo;
    }

    private static String emptyIfNullAndTrim(String s) {
        return s == null ? "" : s.trim();
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
        voteInfo.competent = max(1, min(gwtVoteInfo.competent, 5));
        voteInfo.complete = max(1, min(gwtVoteInfo.complete, 5));
        voteInfo.completeComment = gwtVoteInfo.completeComment;
        voteInfo.date = gwtVoteInfo.date;

        return voteInfo;
    }
}
