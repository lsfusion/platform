package skolkovo.gwt.expert.client;

import com.google.gwt.user.client.Window;
import net.customware.gwt.dispatch.client.DefaultExceptionHandler;
import net.customware.gwt.dispatch.client.standard.StandardDispatchAsync;
import platform.gwt.base.client.BaseFrame;
import skolkovo.api.gwt.shared.VoteInfo;
import skolkovo.gwt.expert.client.ui.ExpertMainWidget;
import skolkovo.gwt.expert.shared.actions.GetVoteInfo;
import skolkovo.gwt.expert.shared.actions.GetVoteInfoResult;
import skolkovo.gwt.expert.shared.actions.SetVoteInfo;

import java.util.Date;

import static skolkovo.api.gwt.shared.Result.VOTED;

public class ExpertFrame extends BaseFrame {
    private static ExpertFrameMessages messages = ExpertFrameMessages.Instance.get();
    private final static StandardDispatchAsync expertService = new StandardDispatchAsync(new DefaultExceptionHandler());

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

        expertService.execute(new GetVoteInfo(voteId), new ErrorAsyncCallback<GetVoteInfoResult>() {
            public void success(final GetVoteInfoResult result) {
                final VoteInfo vi = result.voteInfo;
                if (vi == null) {
                    showErrorPage(null);
                    return;
                }

                setAsRootPane(new ExpertMainWidget(vi) {
                    @Override
                    public boolean onVoted(String voteResult, boolean confirm) {
                        if (VOTED.equals(voteResult)) {
                            if ((VOTE_REVISION_1.equals(vi.revision) &&
                                (bxInCluster.getSelectedIndex() == 0 ||
                                bxInnovative.getSelectedIndex() == 0 ||
                                bxForeign.getSelectedIndex() == 0)) ||
                                    (VOTE_REVISION_2.equals(vi.revision) &&
                                    ((bxCompetitive.getSelectedIndex() == 0 ||
                                    bxCommercePotential.getSelectedIndex() == 0 ||
                                    bxImplement.getSelectedIndex() == 0 ||
                                    bxExpertise.getSelectedIndex() == 0 ||
                                    bxInternationalExperience.getSelectedIndex() == 0 ||
                                    bxEnoughDocuments.getSelectedIndex() == 0)))) {
                                Window.alert(messages.incompletePrompt());
                                return false;
                            }

//                            if (taInnovativeComment.getText().length() < innovativeCommentMaxLength ||
//                                    taCompleteComment.getText().length() < completeCommentMaxLength) {
//                                Window.alert(messages.incompleteComment());
//                                return false;
//                            }
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

                        vi.competitiveAdvantages = bxCompetitive.getSelectedIndex() == 1;
                        vi.competitiveAdvantagesComment = taCompetitiveComment.getText();
                        vi.commercePotential = bxCommercePotential.getSelectedIndex() == 1;
                        vi.commercePotentialComment = taCommercePotentialComment.getText();
                        vi.implement = bxImplement.getSelectedIndex() == 1;
                        vi.implementComment = taImplementComment.getText();
                        vi.expertise = bxExpertise.getSelectedIndex() == 1;
                        vi.expertiseComment = taExpertiseComment.getText();
                        vi.internationalExperience = bxInternationalExperience.getSelectedIndex() == 1;
                        vi.internationalExperienceComment = taInternationalExperienceComment.getText();
                        vi.enoughDocuments = bxEnoughDocuments.getSelectedIndex() == 1;
                        vi.enoughDocumentsComment = taEnoughDocumentsComment.getText();

                        String voteId = getVoteId();
                        if (voteId == null) {
                            showErrorPage(null);
                            return confirm;
                        }

                        expertService.execute(new SetVoteInfo(vi, voteId), new UpdateAsyncCallback());
                        return true;
                    }
                });
            }
        });
    }
}
