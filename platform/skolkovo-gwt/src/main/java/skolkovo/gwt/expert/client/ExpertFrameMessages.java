package skolkovo.gwt.expert.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;

public interface ExpertFrameMessages extends Messages {
    String project();

    String lbInCluster(String clusterName);

    String title();

    String claimer();

    String expert();

    String date();

    String pleasePrompt();

    String votedPrompt();

    String refusedPrompt();

    String connectedPrompt();

    String lbInnovative();

    String lbInnovativeComment();

    String lbForeign();

    String lbCompetent();

    String lbComplete();

    String lbCompleteComment();

    String btnVote();

    String btnRefused();

    String btnConnected();

    String incompletePrompt();

    String incompleteComment();

    String confirmPrompt();

    String footerCaption();

    String pageTitle();

    public static class Instance {
        private static final ExpertFrameMessages instance = (ExpertFrameMessages) GWT.create(ExpertFrameMessages.class);

        public static ExpertFrameMessages get() {
            return instance;
        }
    }
}
