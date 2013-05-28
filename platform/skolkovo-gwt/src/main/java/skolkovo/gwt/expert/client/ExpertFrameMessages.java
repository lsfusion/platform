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

    String lbCompetitive();

    String lbR2DefaultComment();

    String lbCommercePotential();

    String lbImplement();

    String lbExpertise();

    String lbInternationalExperience();

    String lbEnoughDocuments();

    String lbEnoughDocumentsComment();

    String btnVote();

    String btnRefused();

    String btnConnected();

    String connectedQuestion();

    String connectedInfo();

    String incompletePrompt();

    String incompleteComment();

    String commentLengthWarning(String questions);

    String confirmPrompt();

    String footerCaption();

    String pageTitle();

    String voteClosed();

    String symbolsLeft();

    String minSymbols();

    String maxSymbols();

    String noRevisionSpan();

    String closeDialogButton();

    public static class Instance {
        private static final ExpertFrameMessages instance = (ExpertFrameMessages) GWT.create(ExpertFrameMessages.class);

        public static ExpertFrameMessages get() {
            return instance;
        }
    }
}
