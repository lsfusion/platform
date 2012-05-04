package skolkovo.gwt.expertprofile.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;

public interface ExpertProfileMessages extends Messages {
    String title();

    String tabInfo();

    String tabVotes();

    String columnGoToBallot();

    String columnProjectClaimer();

    String columnProjectName();

    String columnProjectCluster();

    String columnCompetent();

    String columnInCluster();

    String columnInnovative();

    String columnComplete();

    String columnForeign();

    String columnCompetitive();

    String columnCommercePotential();

    String columnImplement();

    String columnExpertise();

    String columnInternationalExperience();

    String columnEnoughDocuments();

    String view();

    String showUnvoted();

    String columnVoteResult();

    String columnSentDocs();

    String send();

    String emptyVoteList();

    String name();

    String email();

    @Key("result.voted")
    String resultVoted();

    @Key("result.connected")
    String resultConnected();

    @Key("result.refused")
    String resultRefused();

    @Key("result.opened")
    String resultOpened();

    String columnStartDate();

    String columnEndtDate();

    @Key("result.closed")
    String resultClosed();

    String sentSuccessMessage();

    String sentFailedMessage();

    String sectionExpertDetails();

    String sectionVoteList();

    String sectionForesights();

    String update();

    String commentHint();

    String classHint();

    String classScientific();

    String classTechnical();

    String classBusiness();

    String emptyClassError();

    String emptyForesightsError();

    String expertiseClassPrompt();

    String appTypePropmpt();

    String appTypeExpertise();

    String appTypeGrant();

    String foresightHint();

    public static class Instance {
        private static final ExpertProfileMessages instance = (ExpertProfileMessages) GWT.create(ExpertProfileMessages.class);

        public static ExpertProfileMessages get() {
            return instance;
        }
    }
}
