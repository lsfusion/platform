package skolkovo.gwt.expert.shared.actions;

import net.customware.gwt.dispatch.shared.Action;

public class GetVoteInfo implements Action<GetVoteInfoResult> {
    public String voteId;

    public GetVoteInfo() {}

    public GetVoteInfo(String voteId) {
        this.voteId = voteId;
    }
}
