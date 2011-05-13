package skolkovo.gwt.expert.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import skolkovo.gwt.base.shared.GwtVoteInfo;
import skolkovo.gwt.base.shared.MessageException;

@RemoteServiceRelativePath("ProjectsService")
public interface ExpertService extends RemoteService {
    GwtVoteInfo getVoteInfo(String voteId) throws MessageException;
    void setVoteInfo(GwtVoteInfo voteInfo, String voteId) throws MessageException;

    /**
     * Utility/Convenience class.
     * Use ExpertService.App.getInstance() to access static instance of ProjectsServiceAsync
     */
    public static class App {
        private static final ExpertServiceAsync instance = (ExpertServiceAsync) GWT.create(ExpertService.class);

        public static ExpertServiceAsync getInstance() {
            return instance;
        }
    }
}
