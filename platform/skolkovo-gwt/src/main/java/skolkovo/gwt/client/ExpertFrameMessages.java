package skolkovo.gwt.client;

import com.google.gwt.i18n.client.Messages;

public interface ExpertFrameMessages extends Messages {
    String lbInCluster(String clusterName);
    String title();

    String caption(String expertName, String projectClaimer, String pleasePrompt, String resultPrompt, String date);

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

    String internalServerErrorMessage();

    String incompletePrompt();

    String confirmPrompt();

    String yes();

    String no();
}
