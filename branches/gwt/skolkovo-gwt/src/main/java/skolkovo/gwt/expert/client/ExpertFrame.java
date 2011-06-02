package skolkovo.gwt.expert.client;

import com.google.gwt.user.client.Window;
import platform.gwt.base.client.BaseFrame;
import skolkovo.api.gwt.shared.VoteInfo;
import skolkovo.gwt.expert.client.ui.ExpertMainWidget;

import java.util.Date;

import static skolkovo.api.gwt.shared.Result.VOTED;

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

        expertService.getVoteInfo(voteId, new ErrorAsyncCallback<VoteInfo>() {
            public void onSuccess(final VoteInfo vi) {
                if (vi == null) {
                    showErrorPage(null);
                    return;
                }

                setAsRootPane(new ExpertMainWidget(vi) {
                    @Override
                    public boolean onVoted(String voteResult, boolean confirm) {
                        if (VOTED.equals(voteResult)) {
                            if (bxInCluster.getSelectedIndex() == 0 ||
                                bxInnovative.getSelectedIndex() == 0 ||
                                bxForeign.getSelectedIndex() == 0) {
                                Window.alert(messages.incompletePrompt());
                                return false;
                            }

                            if (taInnovativeComment.getText().isEmpty() ||
                                taCompleteComment.getText().isEmpty()) {
                                Window.alert(messages.incompleteComment());
                                return false;
                            }
                        }

                        if (confirm && !Window.confirm(messages.confirmPrompt())) {
                            return false;
                        }

                        VoteInfo vi = new VoteInfo();

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
                            return confirm;
                        }

                        expertService.setVoteInfo(vi, voteId, new UpdateAsyncCallback());
                        return true;
                    }
                });
            }
        });
    }
}
