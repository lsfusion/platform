package skolkovo.gwt.expert.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.Window;
import net.customware.gwt.dispatch.client.DefaultExceptionHandler;
import net.customware.gwt.dispatch.client.standard.StandardDispatchAsync;
import platform.gwt.base.client.ErrorHandlingCallback;
import platform.gwt.base.client.GwtClientUtils;
import skolkovo.api.gwt.shared.VoteInfo;
import skolkovo.gwt.expert.client.ui.ExpertMainWidget;
import skolkovo.gwt.expert.shared.actions.GetVoteInfo;
import skolkovo.gwt.expert.shared.actions.GetVoteInfoResult;
import skolkovo.gwt.expert.shared.actions.SetVoteInfo;

import java.util.Date;

import static skolkovo.api.gwt.shared.Result.VOTED;

public class ExpertFrame implements EntryPoint {
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
        final String voteId = getVoteId();
        expertService.execute(new GetVoteInfo(voteId), new ErrorHandlingCallback<GetVoteInfoResult>() {
            public void success(final GetVoteInfoResult result) {
                final VoteInfo vi = result.voteInfo;

                GwtClientUtils.setAsRootPane(new ExpertMainWidget(vi) {
                    @Override
                    public boolean onVoted(String voteResult, boolean confirm) {
                        if (VOTED.equals(voteResult)) {
                            if ((VOTE_REVISION_1.equals(vi.revision.trim()) &&
                                 (bxInCluster.getSelectedIndex() == 0 ||
                                  bxInnovative.getSelectedIndex() == 0 ||
                                  bxForeign.getSelectedIndex() == 0)) ||
                                (VOTE_REVISION_2.equals(vi.revision.trim()) &&
                                 ((bxCompetitive.getSelectedIndex() == 0 ||
                                   bxCommercePotential.getSelectedIndex() == 0 ||
                                   bxImplement.getSelectedIndex() == 0 ||
                                   bxExpertise.getSelectedIndex() == 0 ||
                                   bxInternationalExperience.getSelectedIndex() == 0 ||
                                   bxEnoughDocuments.getSelectedIndex() == 0)))) {
                                Window.alert(messages.incompletePrompt());
                                return false;
                            }

                            if (VOTE_REVISION_2.equals(vi.revision.trim())) {
                                String questions = "";
                                if (taCompetitiveComment.getText().length() > commentMaxLength || taCompetitiveComment.getText().length() < commentMinLength) {
                                    questions += "1";
                                }
                                if (taCommercePotentialComment.getText().length() > commentMaxLength || taCommercePotentialComment.getText().length() < commentMinLength) {
                                    questions += questions.isEmpty() ? "2" : ", 2";
                                }
                                if (taImplementComment.getText().length() > commentMaxLength || taImplementComment.getText().length() < commentMinLength) {
                                    questions += questions.isEmpty() ? "3" : ", 3";
                                }
                                if (taExpertiseComment.getText().length() > commentMaxLength || taExpertiseComment.getText().length() < commentMinLength) {
                                    questions += questions.isEmpty() ? "4" : ", 4";
                                }
                                if (taInternationalExperienceComment.getText().length() > commentMaxLength || taInternationalExperienceComment.getText().length() < commentMinLength) {
                                    questions += questions.isEmpty() ? "5" : ", 5";
                                }
                                if (taEnoughDocumentsComment.getText().length() > commentMaxLength || taEnoughDocumentsComment.getText().length() < commentMinLength) {
                                    questions += questions.isEmpty() ? "6" : ", 6";
                                }
                                if (!questions.isEmpty()) {
                                    Window.alert(messages.commentLengthWarning(questions));
                                    return false;
                                }
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

                        expertService.execute(new SetVoteInfo(vi, voteId), new ErrorHandlingCallback() {
                            @Override
                            public void success(Object result) {
                                update();
                            }
                        });

                        return true;
                    }
                });
            }
        });
    }
}
