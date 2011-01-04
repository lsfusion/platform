package skolkovo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import skolkovo.gwt.shared.GwtVoteInfo;

@RemoteServiceRelativePath("ProjectsService")
public interface ExpertService extends RemoteService {
    GwtVoteInfo getVoteInfo(int voteId);
    void setVoteInfo(GwtVoteInfo voteInfo, int voteId);

    /**
     * Utility/Convenience class.
     * Use ExpertService.App.getInstance() to access static instance of ProjectsServiceAsync
     */
    public static class App {
        private static final ExpertServiceAsync ourInstance = (ExpertServiceAsync) GWT.create(ExpertService.class);

        public static ExpertServiceAsync getInstance() {
            return ourInstance;
        }
    }
}
