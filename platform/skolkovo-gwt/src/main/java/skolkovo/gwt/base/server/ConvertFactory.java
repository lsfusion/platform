package skolkovo.gwt.base.server;

import skolkovo.ProfileInfo;
import skolkovo.VoteInfo;
import skolkovo.gwt.base.shared.GwtProfileInfo;
import skolkovo.gwt.base.shared.GwtVoteInfo;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class ConvertFactory {
    public static GwtVoteInfo toGwtVoteInfo(VoteInfo voteInfo) {
        GwtVoteInfo gwtVoteInfo = new GwtVoteInfo();

        gwtVoteInfo.voteId = voteInfo.voteId;
        gwtVoteInfo.linkHash = voteInfo.linkHash;
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

    public static GwtProfileInfo toGwtProfileInfo(ProfileInfo profileInfo) {
        GwtProfileInfo gwtProfileInfo = new GwtProfileInfo();

        gwtProfileInfo.expertEmail = profileInfo.expertEmail;
        gwtProfileInfo.expertName = profileInfo.expertName;
        gwtProfileInfo.voteInfos = new GwtVoteInfo[profileInfo.voteInfos.length];
        for (int i = 0; i < profileInfo.voteInfos.length; ++i) {
            gwtProfileInfo.voteInfos[i] = toGwtVoteInfo(profileInfo.voteInfos[i]);
        }

        return gwtProfileInfo;
    }

    public static VoteInfo toVoteInfo(GwtVoteInfo gwtVoteInfo) {
        VoteInfo voteInfo = new VoteInfo();

        voteInfo.voteId = gwtVoteInfo.voteId;
        voteInfo.linkHash = gwtVoteInfo.linkHash;
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

    private static String emptyIfNullAndTrim(String s) {
        return s == null ? "" : s.trim();
    }
}
