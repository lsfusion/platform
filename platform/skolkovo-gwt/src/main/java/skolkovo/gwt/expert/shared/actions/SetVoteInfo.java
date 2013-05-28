package skolkovo.gwt.expert.shared.actions;

import net.customware.gwt.dispatch.shared.Action;
import platform.gwt.base.shared.actions.VoidResult;
import skolkovo.api.gwt.shared.VoteInfo;

public class SetVoteInfo implements Action<VoidResult> {
    public VoteInfo voteInfo;
    public String voteId;

    public SetVoteInfo() {}

    public SetVoteInfo(VoteInfo voteInfo, String voteId) {
        this.voteInfo = voteInfo;
        this.voteId = voteId;
    }
}
