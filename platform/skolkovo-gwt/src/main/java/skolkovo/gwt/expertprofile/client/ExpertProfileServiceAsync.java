package skolkovo.gwt.expertprofile.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import skolkovo.gwt.base.shared.GwtProfileInfo;

public interface ExpertProfileServiceAsync {
    void getProfileInfo(AsyncCallback<GwtProfileInfo> async);

    void sentVoteDocuments(int voteId, AsyncCallback<Void> async);
}
