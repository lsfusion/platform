package skolkovo.gwt.expert.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.*;
import skolkovo.gwt.base.client.BaseMessages;
import skolkovo.gwt.base.shared.GwtVoteInfo;
import skolkovo.gwt.expert.client.ExpertFrameMessages;

import java.util.Date;

public abstract class ExpertMainWidget extends Composite {
    private static final int innovativeCommentMaxLength = 1000;
    private static final int completeCommentMaxLength = 300;

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
    @UiField
    Label lbCompleteCounter;
    @UiField
    Label lbCompleteCounterCaption;
    @UiField
    Label lbInnovativeCounter;
    @UiField
    Label lbInnovativeCounterCaption;
    @UiField
    HTMLPanel dataHtmlPanel;

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

        lbInnovativeCounter.setText("1000");
        lbInnovativeCounterCaption.setText(messages.symbolsLeft());

        lbCompleteCounter.setText("300");
        lbCompleteCounterCaption.setText(messages.symbolsLeft());

        voteResultSpan.setInnerHTML(!vi.voteDone
                                    ? messages.pleasePrompt()
                                    : "<b>" +
                                      ("voted".equals(vi.voteResult)
                                       ? messages.votedPrompt()
                                       : "refused".equals(vi.voteResult)
                                         ? messages.refusedPrompt()
                                         : "connected".equals(vi.voteResult)
                                           ? messages.connectedPrompt()
                                           : messages.voteClosed()) + "</b>"
        );

        setupHandlers();

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
        taInnovativeComment.setValue(vi.innovativeComment);
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
        taCompleteComment.setValue(vi.completeComment);
        taCompleteComment.setEnabled(!vi.voteDone);

        bVote.setText(messages.btnVote());
        bRefused.setText(messages.btnRefused());
        bConnected.setText(messages.btnConnected());

        if (vi.voteDone) {
            voteButtonsPanel.setVisible(false);
            Element resultDataRow = dataHtmlPanel.getElementById("resultDataRow");
            if (resultDataRow != null) {
                resultDataRow.removeFromParent();
            }

            if (!"voted".equals(vi.voteResult)) {
                for (int i = 1; ;++i) {
                    Element dataRow = dataHtmlPanel.getElementById("dataRow" + i);
                    if (dataRow == null) break;

                    dataRow.removeFromParent();
                }
            }
        }
    }

    private void setupHandlers() {
        LimitedTextHandler innovativeLimitedHandler = new LimitedTextHandler(taInnovativeComment, lbInnovativeCounter, innovativeCommentMaxLength);
        LimitedTextHandler completeLimitedHandler = new LimitedTextHandler(taCompleteComment, lbCompleteCounter, completeCommentMaxLength);

        taInnovativeComment.addValueChangeHandler(innovativeLimitedHandler);
        taInnovativeComment.addKeyboardListener(innovativeLimitedHandler);
        taCompleteComment.addValueChangeHandler(completeLimitedHandler);
        taCompleteComment.addKeyboardListener(completeLimitedHandler);

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

    private static class LimitedTextHandler implements ValueChangeHandler<String>, KeyboardListener {
        private final TextArea taText;
        private final Label lbCounter;
        private final int maxLength;

        public LimitedTextHandler(TextArea taText, Label lbCounter, int maxLength) {
            this.taText = taText;
            this.lbCounter = lbCounter;
            this.maxLength = maxLength;
        }

        @Override
        public void onValueChange(ValueChangeEvent<String> stringValueChangeEvent) {
            limitText();
        }

        @Override
        public void onKeyDown(Widget sender, char keyCode, int modifiers) {
            limitText();
        }

        @Override
        public void onKeyPress(Widget sender, char keyCode, int modifiers) {
            limitText();
        }

        @Override
        public void onKeyUp(Widget sender, char keyCode, int modifiers) {
            limitText();
        }

        private void limitText() {
            String text = taText.getValue();
            if (text == null) return;
            if (text.length() > maxLength) {
                text = text.substring(0, maxLength);
                taText.setValue(text);
                return;
            }

            lbCounter.setText("" + (maxLength - text.length()));
        }
    }
}