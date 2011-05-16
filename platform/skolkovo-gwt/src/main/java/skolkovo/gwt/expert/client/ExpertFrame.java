package skolkovo.gwt.expert.client;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.RootPanel;
import skolkovo.gwt.base.client.BaseFrame;
import skolkovo.gwt.base.shared.GwtVoteInfo;
import skolkovo.gwt.expert.client.ui.ExpertMainWidget;

import java.util.Date;

public class ExpertFrame extends BaseFrame {
    private static ExpertFrameMessages messages = ExpertFrameMessages.Instance.get();
    private static final ExpertServiceAsync expertService = ExpertService.App.getInstance();

    public void onModuleLoad() {
        Window.setTitle(messages.pageTitle());
        update();
    }

    public String getVoteId() {
        try {
            return Window.Location.getParameter("voteId");
        } catch (Exception nfe) {
            return null;
        }
    }

    protected void update() {
        String voteId = getVoteId();
        if (voteId == null) {
            showErrorPage(null);
            return;
        }

        expertService.getVoteInfo(voteId, new ErrorAsyncCallback<GwtVoteInfo>() {
            public void onSuccess(GwtVoteInfo vi) {
                if (vi == null) {
                    showErrorPage(null);
                    return;
                }

                RootPanel.get().clear();
                final GwtVoteInfo vi1 = vi;
                RootPanel.get().add(new ExpertMainWidget(vi1) {
                    @Override
                    public void onVoted(String voteResult, boolean confirm) {
                        if ("voted".equals(voteResult) &&
                            (bxInCluster.getSelectedIndex() == 0 ||
                             bxInnovative.getSelectedIndex() == 0 ||
                             bxForeign.getSelectedIndex() == 0)) {
                            Window.alert(messages.incompletePrompt());
                            return;
                        }

                        if ("voted".equals(voteResult) &&
                            (taInnovativeComment.getText().isEmpty() ||
                             taCompleteComment.getText().isEmpty())) {
                            Window.alert(messages.incompleteComment());
                            return;
                        }

                        if (confirm && !Window.confirm(messages.confirmPrompt())) {
                            return;
                        }

                        GwtVoteInfo vi = new GwtVoteInfo();

                        vi.date = new Date();
                        vi.voteResult = voteResult;

                        vi.inCluster = bxInCluster.getSelectedIndex() == 1;
                        vi.innovative = bxInnovative.getSelectedIndex() == 1;
                        vi.innovativeComment = taInnovativeComment.getText();
                        vi.foreign = bxForeign.getSelectedIndex() == 1;
                        vi.competent = bxCompetent.getSelectedIndex() + 1;
                        vi.complete = bxComplete.getSelectedIndex() + 1;
                        vi.completeComment = taCompleteComment.getText();

                        String voteId = getVoteId();
                        if (voteId == null) {
                            showErrorPage(null);
                            return;
                        }

                        RootPanel.get().clear();
                        RootPanel.get().add(new HTMLPanel("Loading..."));

                        expertService.setVoteInfo(vi, voteId, new UpdateAsyncCallback());
                    }
                });
            }
        });
    }
}
