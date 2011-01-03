package skolkovo.gwt.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import skolkovo.gwt.shared.GwtVoteInfo;

public interface ExpertServiceAsync {
    void getVoteInfo(String login, int voteId, AsyncCallback<GwtVoteInfo> async);
}
