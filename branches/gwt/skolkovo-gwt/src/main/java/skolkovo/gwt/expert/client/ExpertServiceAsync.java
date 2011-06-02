package skolkovo.gwt.expert.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import skolkovo.api.gwt.shared.VoteInfo;

public interface ExpertServiceAsync {
    void getVoteInfo(String voteId, AsyncCallback<VoteInfo> async);

    void setVoteInfo(VoteInfo voteInfo, String voteId, AsyncCallback<Void> async);
}
