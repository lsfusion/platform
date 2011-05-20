package skolkovo.gwt.expertprofile.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import skolkovo.api.serialization.ProfileInfo;
import skolkovo.gwt.base.shared.MessageException;

@RemoteServiceRelativePath("ExpertProfileService")
public interface ExpertProfileService extends RemoteService {
    ProfileInfo getProfileInfo() throws MessageException;
    void sentVoteDocuments(int voteId) throws MessageException;

    /**
     * Utility/Convenience class.
     * Use ExpertAreaService.App.getInstance() to access static instance of ExpertAreaServiceAsync
     */
    public static class App {
        private static final ExpertProfileServiceAsync instance = (ExpertProfileServiceAsync) GWT.create(ExpertProfileService.class);

        public static ExpertProfileServiceAsync getInstance() {
            return instance;
        }
    }
}
