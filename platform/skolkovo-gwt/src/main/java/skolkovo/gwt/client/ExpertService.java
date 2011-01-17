package skolkovo.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import skolkovo.gwt.shared.GwtVoteInfo;
import skolkovo.gwt.shared.MessageException;

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

    public static class Messages {
        private static final ExpertFrameMessages instance = (ExpertFrameMessages) GWT.create(ExpertFrameMessages.class);

        public static ExpertFrameMessages getInstance() {
            return instance;
        }
    }
}
