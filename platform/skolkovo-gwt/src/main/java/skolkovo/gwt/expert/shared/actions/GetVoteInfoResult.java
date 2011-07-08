package skolkovo.gwt.expert.shared.actions;

import net.customware.gwt.dispatch.shared.Result;
import skolkovo.api.gwt.shared.VoteInfo;

public class GetVoteInfoResult implements Result {
    public VoteInfo voteInfo;

    public GetVoteInfoResult() {}

    public GetVoteInfoResult(VoteInfo voteInfo) {
        this.voteInfo = voteInfo;
    }
}
