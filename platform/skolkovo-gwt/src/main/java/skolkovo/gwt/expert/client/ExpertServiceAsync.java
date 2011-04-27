package skolkovo.gwt.expert.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import skolkovo.gwt.expert.shared.GwtVoteInfo;

public interface ExpertServiceAsync {
    void getVoteInfo(String voteId, AsyncCallback<GwtVoteInfo> async);

    void setVoteInfo(GwtVoteInfo voteInfo, String voteId, AsyncCallback<Void> async);
}
