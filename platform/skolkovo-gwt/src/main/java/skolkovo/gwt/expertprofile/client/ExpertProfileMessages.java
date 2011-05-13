package skolkovo.gwt.expertprofile.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;

public interface ExpertProfileMessages extends Messages {
    String title();

    String columnGoToBallot();

    String columnProjectClaimer();

    String columnProjectName();

    String columnProjectCluster();

    String columnCompetent();

    String columnInCluster();

    String columnInnovative();

    String columnComplete();

    String columnForeign();

    String view();

    String showUnvoted();

    String columnVoteDone();

    String columnSentDocs();

    String send();

    String emptyVoteList();

    String name();

    String email();

    public static class Instance {
        private static final ExpertProfileMessages instance = (ExpertProfileMessages) GWT.create(ExpertProfileMessages.class);

        public static ExpertProfileMessages get() {
            return instance;
        }
    }
}
