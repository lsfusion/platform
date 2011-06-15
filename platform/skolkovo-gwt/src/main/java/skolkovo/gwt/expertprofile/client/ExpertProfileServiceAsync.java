package skolkovo.gwt.expertprofile.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import skolkovo.api.gwt.shared.ProfileInfo;

public interface ExpertProfileServiceAsync {
    void getProfileInfo(AsyncCallback<ProfileInfo> async);
    void sentVoteDocuments(int voteId, AsyncCallback<Void> async);
}
