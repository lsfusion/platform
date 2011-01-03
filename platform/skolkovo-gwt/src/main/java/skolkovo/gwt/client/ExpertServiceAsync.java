package skolkovo.gwt.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import skolkovo.gwt.shared.GwtVoteInfo;

public interface ExpertServiceAsync {
    void getVoteInfo(int expertId, int projectId, AsyncCallback<GwtVoteInfo> async);
}
