package skolkovo.gwt.server;

import skolkovo.VoteInfo;
import skolkovo.gwt.shared.GwtVoteInfo;

public class VoteFactory {
    public static GwtVoteInfo toGwtVoteInfo(VoteInfo voteInfo) {
        GwtVoteInfo gwtVoteInfo = new GwtVoteInfo();

        gwtVoteInfo.expertName = voteInfo.expertName;
        gwtVoteInfo.projectClaimer = voteInfo.projectClaimer;
        gwtVoteInfo.projectName = voteInfo.projectName;
        gwtVoteInfo.projectCluster = voteInfo.projectCluster;
        gwtVoteInfo.connected = voteInfo.connected;
        gwtVoteInfo.inCluster = voteInfo.inCluster;
        gwtVoteInfo.innovative = voteInfo.innovative;
        gwtVoteInfo.innovativeComment = voteInfo.innovativeComment;
        gwtVoteInfo.foreign = voteInfo.foreign;
        gwtVoteInfo.competent = voteInfo.competent;
        gwtVoteInfo.complete = voteInfo.complete;
        gwtVoteInfo.completeComment = voteInfo.completeComment;

        return gwtVoteInfo;
    }

    public static VoteInfo toVoteInfo(GwtVoteInfo gwtVoteInfo) {
        VoteInfo voteInfo = new VoteInfo();

        voteInfo.expertName = gwtVoteInfo.expertName;
        voteInfo.projectClaimer = gwtVoteInfo.projectClaimer;
        voteInfo.projectName = gwtVoteInfo.projectName;
        voteInfo.projectCluster = gwtVoteInfo.projectCluster;
        voteInfo.connected = gwtVoteInfo.connected;
        voteInfo.inCluster = gwtVoteInfo.inCluster;
        voteInfo.innovative = gwtVoteInfo.innovative;
        voteInfo.innovativeComment = gwtVoteInfo.innovativeComment;
        voteInfo.foreign = gwtVoteInfo.foreign;
        voteInfo.competent = gwtVoteInfo.competent;
        voteInfo.complete = gwtVoteInfo.complete;
        voteInfo.completeComment = gwtVoteInfo.completeComment;

        return voteInfo;
    }
}
