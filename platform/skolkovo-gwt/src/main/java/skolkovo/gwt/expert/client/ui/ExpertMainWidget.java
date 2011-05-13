package skolkovo.gwt.expert.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;
import skolkovo.gwt.base.client.BaseMessages;
import skolkovo.gwt.base.shared.GwtVoteInfo;
import skolkovo.gwt.expert.client.ExpertFrameMessages;

import java.util.Date;

public abstract class ExpertMainWidget extends Composite {
    interface ExpertMainWidgetUiBinder extends UiBinder<Widget, ExpertMainWidget> {}
    private static ExpertMainWidgetUiBinder uiBinder = GWT.create(ExpertMainWidgetUiBinder.class);

    private static ExpertFrameMessages messages = ExpertFrameMessages.Instance.get();
    private static BaseMessages baseMessages = BaseMessages.Instance.get();

    @UiField
    SpanElement projectSpan;
    @UiField
    SpanElement claimerSpan;
    @UiField
    SpanElement expertSpan;
    @UiField
    SpanElement dateSpan;
    @UiField
    Anchor logoffLink;
    @UiField
    Label logoffNotice;
    @UiField
    SpanElement voteResultSpan;
    @UiField
    VerticalPanel voteDataPanel;
    @UiField
    public ListBox bxInCluster;
    @UiField
    Label lbInCluster;
    @UiField
    Label lbInnovative;
    @UiField
    public ListBox bxInnovative;
    @UiField
    public TextArea taInnovativeComment;
    @UiField
    Label lbInnovativeComment;
    @UiField
    Label lbForeign;
    @UiField
    public ListBox bxForeign;
    @UiField
    Label lbCompetent;
    @UiField
    public ListBox bxCompetent;
    @UiField
    Label lbComplete;
    @UiField
    public ListBox bxComplete;
    @UiField
    Label lbCompleteComment;
    @UiField
    public TextArea taCompleteComment;
    @UiField
    Button bVote;
    @UiField
    Anchor bRefused;
    @UiField
    Anchor bConnected;
    @UiField
    VerticalPanel voteButtonsPanel;
    @UiField
    SpanElement footerSpan;
    @UiField
    SpanElement titleSpan;
    @UiField
    SpanElement projectLabelSpan;
    @UiField
    SpanElement claimerLabelSpan;
    @UiField
    SpanElement expertLabelSpan;
    @UiField
    SpanElement dateLabelSpan;

    public ExpertMainWidget(GwtVoteInfo vi) {
        initWidget(uiBinder.createAndBindUi(this));

        Date voteDate = vi.date == null ? new Date() : vi.date;

        titleSpan.setInnerText(messages.title());
        projectLabelSpan.setInnerText(messages.project());
        projectSpan.setInnerText(vi.projectName);
        claimerLabelSpan.setInnerText(messages.claimer());
        claimerSpan.setInnerText(vi.projectClaimer);
        expertLabelSpan.setInnerText(messages.expert());
        expertSpan.setInnerText(vi.expertName);
        dateLabelSpan.setInnerHTML(messages.date());
        dateSpan.setInnerText(DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_SHORT).format(voteDate));
        logoffNotice.setText(baseMessages.logoffNotice());
        logoffLink.setText(baseMessages.here());
        footerSpan.setInnerHTML(messages.footerCaption());

        voteResultSpan.setInnerHTML(!vi.voteDone
                                    ? messages.pleasePrompt()
                                    : "<b>" +
                                      ("voted".equals(vi.voteResult)
                                       ? messages.votedPrompt()
                                       : "refused".equals(vi.voteResult)
                                         ? messages.refusedPrompt()
                                         : "connected".equals(vi.voteResult)
                                           ? messages.connectedPrompt()
                                           : "") + "</b>"
        );

        if (vi.voteDone) {
            logoffLink.setVisible(false);
            logoffNotice.setVisible(false);
        }

        lbInCluster.setText(messages.lbInCluster(vi.projectCluster));
        bxInCluster.setEnabled(!vi.voteDone);
        addListBoxBooleanItems(bxInCluster, vi.voteDone ? (vi.inCluster ? 1 : 2) : 0);

        lbInnovative.setText(messages.lbInnovative());
        bxInnovative.setEnabled(!vi.voteDone);
        addListBoxBooleanItems(bxInnovative, vi.voteDone ? (vi.innovative ? 1 : 2) : 0);

        lbInnovativeComment.setText(messages.lbInnovativeComment());
        taInnovativeComment.setText(vi.innovativeComment);
        taInnovativeComment.setEnabled(!vi.voteDone);

        lbForeign.setText(messages.lbForeign());
        addListBoxBooleanItems(bxForeign, vi.voteDone ? (vi.foreign ? 1 : 2) : 0);
        bxForeign.setEnabled(!vi.voteDone);

        lbCompetent.setText(messages.lbCompetent());
        for (int i = 1; i <= 5; ++i) {
            bxCompetent.addItem("" + i);
        }
        bxCompetent.setItemSelected(vi.competent - 1, true);
        bxCompetent.setEnabled(!vi.voteDone);

        lbComplete.setText(messages.lbComplete());
        for (int i = 1; i <= 5; ++i) {
            bxComplete.addItem("" + i);
        }
        bxComplete.setItemSelected(vi.complete - 1, true);
        bxComplete.setEnabled(!vi.voteDone);

        lbCompleteComment.setText(messages.lbCompleteComment());
        taCompleteComment.setText(vi.completeComment);
        taCompleteComment.setEnabled(!vi.voteDone);

        bVote.setText(messages.btnVote());
        bRefused.setText(messages.btnRefused());
        bConnected.setText(messages.btnConnected());

        if (vi.voteDone) {
            if (!"voted".equals(vi.voteResult)) {
                voteDataPanel.setVisible(false);
            }
            voteButtonsPanel.setVisible(false);
            footerSpan.setInnerText("");
            bVote.setVisible(false);
        }

        logoffLink.addClickHandler(new VoteHandler("refused", false));
        bVote.addClickHandler(new VoteHandler("voted", true));
        bRefused.addClickHandler(new VoteHandler("refused", true));
        bConnected.addClickHandler(new VoteHandler("connected", true));
    }

    private void addListBoxBooleanItems(ListBox listBox, int defaultValue) {
        listBox.addItem("");
        listBox.addItem(baseMessages.yes());
        listBox.addItem(baseMessages.no());
        listBox.setItemSelected(defaultValue, true);
    }

    public abstract void onVoted(String voteResult, boolean confirm);

    private class VoteHandler implements ClickHandler {
        private final String voteResult;
        private final boolean confirm;

        public VoteHandler(String voteResult, boolean confirm) {
            this.voteResult = voteResult;
            this.confirm = confirm;
        }

        @Override
        public void onClick(ClickEvent event) {
            onVoted(voteResult, confirm);
        }
    }
}