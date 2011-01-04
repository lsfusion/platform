package skolkovo.gwt.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import skolkovo.gwt.shared.GwtVoteInfo;

public interface ExpertServiceAsync {
    void getVoteInfo(int voteId, AsyncCallback<GwtVoteInfo> async);

    void setVoteInfo(GwtVoteInfo voteInfo, int voteId, AsyncCallback<Void> async);
}
