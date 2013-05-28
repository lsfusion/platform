package skolkovo.gwt.expert.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.*;
import skolkovo.api.gwt.shared.VoteInfo;
import platform.gwt.base.client.BaseMessages;
import skolkovo.gwt.expert.client.ExpertFrameMessages;

import java.util.Date;

import static skolkovo.api.gwt.shared.Result.*;

public abstract class ExpertMainWidget extends Composite {
    public static final int commentMaxLength = 1000;
    public static final int commentMinLength = 100;

    public static final String VOTE_REVISION_1 = "R1";
    public static final String VOTE_REVISION_2 = "R2";

    interface ExpertMainWidgetUiBinder extends UiBinder<Widget, ExpertMainWidget> {}
    private static ExpertMainWidgetUiBinder uiBinder = GWT.create(ExpertMainWidgetUiBinder.class);

    private static ExpertMainWidgetCSSBundle cssBundle = ExpertMainWidgetCSSBundle.INSTANCE;

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
    Anchor logoutLink;
    @UiField
    Label logoutNotice;
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
    Button bRefused;
    @UiField
    Button bConnected;
    @UiField
    Anchor connectedQuestionLink;
    @UiField
    HorizontalPanel voteButtonsPanel;
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
    @UiField
    Label loadingSpan;
    @UiField
    HorizontalPanel loadingPanel;
    @UiField
    Image imgLogo;
    @UiField
    Label lbCompetitive;
    @UiField
    public ListBox bxCompetitive;
    @UiField
    Label lbCompetitiveMinCounterCaption;
    @UiField
    Label lbCompetitiveMinCounter;
    @UiField
    Label lbCompetitiveMaxCounterCaption;
    @UiField
    Label lbCompetitiveMaxCounter;
    @UiField
    Label lbCompetitiveCounterCaption;
    @UiField
    Label lbCompetitiveComment;
    @UiField
    public TextArea taCompetitiveComment;
    @UiField
    Label lbCommercePotential;
    @UiField
    public ListBox bxCommercePotential;
    @UiField
    Label lbCommercePotentialMinCounterCaption;
    @UiField
    Label lbCommercePotentialMinCounter;
    @UiField
    Label lbCommercePotentialMaxCounterCaption;
    @UiField
    Label lbCommercePotentialMaxCounter;
    @UiField
    Label lbCommercePotentialCounterCaption;
    @UiField
    Label lbCommercePotentialComment;
    @UiField
    public TextArea taCommercePotentialComment;
    @UiField
    Label lbImplement;
    @UiField
    public ListBox bxImplement;
    @UiField
    Label lbImplementMinCounterCaption;
    @UiField
    Label lbImplementMinCounter;
    @UiField
    Label lbImplementMaxCounterCaption;
    @UiField
    Label lbImplementMaxCounter;
    @UiField
    Label lbImplementCounterCaption;
    @UiField
    Label lbImplementComment;
    @UiField
    public TextArea taImplementComment;
    @UiField
    Label lbExpertise;
    @UiField
    public ListBox bxExpertise;
    @UiField
    Label lbExpertiseMinCounterCaption;
    @UiField
    Label lbExpertiseMinCounter;
    @UiField
    Label lbExpertiseMaxCounterCaption;
    @UiField
    Label lbExpertiseMaxCounter;
    @UiField
    Label lbExpertiseCounterCaption;
    @UiField
    Label lbExpertiseComment;
    @UiField
    public TextArea taExpertiseComment;
    @UiField
    Label lbInternationalExperience;
    @UiField
    public ListBox bxInternationalExperience;
    @UiField
    Label lbInternationalExperienceMinCounterCaption;
    @UiField
    Label lbInternationalExperienceMinCounter;
    @UiField
    Label lbInternationalExperienceMaxCounterCaption;
    @UiField
    Label lbInternationalExperienceMaxCounter;
    @UiField
    Label lbInternationalExperienceCounterCaption;
    @UiField
    Label lbInternationalExperienceComment;
    @UiField
    public TextArea taInternationalExperienceComment;
    @UiField
    Label lbEnoughDocuments;
    @UiField
    public ListBox bxEnoughDocuments;
    @UiField
    Label lbEnoughDocumentsMinCounterCaption;
    @UiField
    Label lbEnoughDocumentsMinCounter;
    @UiField
    Label lbEnoughDocumentsMaxCounterCaption;
    @UiField
    Label lbEnoughDocumentsMaxCounter;
    @UiField
    Label lbEnoughDocumentsCounterCaption;
    @UiField
    Label lbEnoughDocumentsComment;
    @UiField
    public TextArea taEnoughDocumentsComment;
    @UiField
    HorizontalPanel noRevisionPanel;
    @UiField
    Label noRevisionSpan;

    public ExpertMainWidget(VoteInfo vi) {
        initWidget(uiBinder.createAndBindUi(this));

        cssBundle.css().ensureInjected();

        Date voteDate = vi.date == null ? new Date() : vi.date;

        imgLogo.setUrl(GWT.getModuleBaseURL() + "images/logo_ballot.png");

        titleSpan.setInnerText(messages.title());
        projectLabelSpan.setInnerText(messages.project());
        projectSpan.setInnerText(vi.projectName);
        claimerLabelSpan.setInnerText(messages.claimer());
        claimerSpan.setInnerText(vi.projectClaimer);
        expertLabelSpan.setInnerText(messages.expert());
        expertSpan.setInnerText(vi.expertName);
        dateLabelSpan.setInnerHTML(messages.date());
        dateSpan.setInnerText(DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_SHORT).format(voteDate));
        logoutNotice.setText(baseMessages.logoutNotice());
        logoutLink.setText(baseMessages.here());
        footerSpan.setInnerHTML(messages.footerCaption());
        loadingSpan.setText(baseMessages.loading());
        noRevisionSpan.setText(messages.noRevisionSpan());

        lbInnovativeCounter.setText(String.valueOf(commentMinLength));
        lbInnovativeCounterCaption.setText(messages.symbolsLeft());

        lbCompleteCounter.setText(String.valueOf(commentMinLength));
        lbCompleteCounterCaption.setText(messages.symbolsLeft());

        lbCompetitiveMinCounterCaption.setText(messages.minSymbols());
        lbCompetitiveMinCounter.setText(String.valueOf(commentMinLength));
        lbCompetitiveMaxCounterCaption.setText(messages.maxSymbols());
        lbCompetitiveMaxCounter.setText(String.valueOf(commentMaxLength));
        lbCompetitiveCounterCaption.setText(messages.symbolsLeft());

        lbCommercePotentialMinCounterCaption.setText(messages.minSymbols());
        lbCommercePotentialMinCounter.setText(String.valueOf(commentMinLength));
        lbCommercePotentialMaxCounterCaption.setText(messages.maxSymbols());
        lbCommercePotentialMaxCounter.setText(String.valueOf(commentMaxLength));
        lbCommercePotentialCounterCaption.setText(messages.symbolsLeft());

        lbImplementMinCounterCaption.setText(messages.minSymbols());
        lbImplementMinCounter.setText(String.valueOf(commentMinLength));
        lbImplementMaxCounterCaption.setText(messages.maxSymbols());
        lbImplementMaxCounter.setText(String.valueOf(commentMaxLength));
        lbImplementCounterCaption.setText(messages.symbolsLeft());

        lbExpertiseMinCounterCaption.setText(messages.minSymbols());
        lbExpertiseMinCounter.setText(String.valueOf(commentMinLength));
        lbExpertiseMaxCounterCaption.setText(messages.maxSymbols());
        lbExpertiseMaxCounter.setText(String.valueOf(commentMaxLength));
        lbExpertiseCounterCaption.setText(messages.symbolsLeft());

        lbInternationalExperienceMinCounterCaption.setText(messages.minSymbols());
        lbInternationalExperienceMinCounter.setText(String.valueOf(commentMinLength));
        lbInternationalExperienceMaxCounterCaption.setText(messages.maxSymbols());
        lbInternationalExperienceMaxCounter.setText(String.valueOf(commentMaxLength));
        lbInternationalExperienceCounterCaption.setText(messages.symbolsLeft());

        lbEnoughDocumentsMinCounterCaption.setText(messages.minSymbols());
        lbEnoughDocumentsMinCounter.setText(String.valueOf(commentMinLength));
        lbEnoughDocumentsMaxCounterCaption.setText(messages.maxSymbols());
        lbEnoughDocumentsMaxCounter.setText(String.valueOf(commentMaxLength));
        lbEnoughDocumentsCounterCaption.setText(messages.symbolsLeft());

        voteResultSpan.setInnerHTML(!vi.voteDone
                                    ? messages.pleasePrompt()
                                    : "<b>" +
                                      (VOTED.equals(vi.voteResult)
                                       ? messages.votedPrompt()
                                       : REFUSED.equals(vi.voteResult)
                                         ? messages.refusedPrompt()
                                         : CONNECTED.equals(vi.voteResult)
                                           ? messages.connectedPrompt()
                                           : messages.voteClosed()) + "</b>"
        );

        setupHandlers();

        if (vi.voteDone) {
            logoutLink.setVisible(false);
            logoutNotice.setVisible(false);
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

        lbCompetitive.setText(messages.lbCompetitive());
        addListBoxBooleanItems(bxCompetitive, vi.voteDone ? (vi.competitiveAdvantages ? 1 : 2) : 0);
        bxCompetitive.setEnabled(!vi.voteDone);
        lbCompetitiveComment.setText(messages.lbR2DefaultComment());
        taCompetitiveComment.setText(vi.competitiveAdvantagesComment);
        taCompetitiveComment.setEnabled(!vi.voteDone);

        lbCommercePotential.setText(messages.lbCommercePotential());
        addListBoxBooleanItems(bxCommercePotential, vi.voteDone ? (vi.commercePotential ? 1 : 2) : 0);
        bxCommercePotential.setEnabled(!vi.voteDone);
        lbCommercePotentialComment.setText(messages.lbR2DefaultComment());
        taCommercePotentialComment.setText(vi.commercePotentialComment);
        taCommercePotentialComment.setEnabled(!vi.voteDone);

        lbImplement.setText(messages.lbImplement());
        addListBoxBooleanItems(bxImplement, vi.voteDone ? (vi.implement ? 1 : 2) : 0);
        bxImplement.setEnabled(!vi.voteDone);
        lbImplementComment.setText(messages.lbR2DefaultComment());
        taImplementComment.setText(vi.implementComment);
        taImplementComment.setEnabled(!vi.voteDone);

        lbExpertise.setText(messages.lbExpertise());
        addListBoxBooleanItems(bxExpertise, vi.voteDone ? (vi.expertise ? 1 : 2) : 0);
        bxExpertise.setEnabled(!vi.voteDone);
        lbExpertiseComment.setText(messages.lbR2DefaultComment());
        taExpertiseComment.setText(vi.expertiseComment);
        taExpertiseComment.setEnabled(!vi.voteDone);

        lbInternationalExperience.setText(messages.lbInternationalExperience());
        addListBoxBooleanItems(bxInternationalExperience, vi.voteDone ? (vi.internationalExperience ? 1 : 2) : 0);
        bxInternationalExperience.setEnabled(!vi.voteDone);
        lbInternationalExperienceComment.setText(messages.lbR2DefaultComment());
        taInternationalExperienceComment.setText(vi.internationalExperienceComment);
        taInternationalExperienceComment.setEnabled(!vi.voteDone);

        lbEnoughDocuments.setText(messages.lbEnoughDocuments());
        addListBoxBooleanItems(bxEnoughDocuments, vi.voteDone ? (vi.enoughDocuments ? 1 : 2) : 0);
        bxEnoughDocuments.setEnabled(!vi.voteDone);
        lbEnoughDocumentsComment.setText(messages.lbEnoughDocumentsComment());
        taEnoughDocumentsComment.setText(vi.enoughDocumentsComment);
        taEnoughDocumentsComment.setEnabled(!vi.voteDone);

        bVote.setText(messages.btnVote());
        bRefused.setText(messages.btnRefused());
        bConnected.setText(messages.btnConnected());
        if ("ru".equals(LocaleInfo.getCurrentLocale().getLocaleName()))
            connectedQuestionLink.setText(messages.connectedQuestion());

        if (vi.voteDone) {
            hideDataRows(!VOTED.equals(vi.voteResult));
        }

        hideRowsAccordingToRevision(vi);
    }

    private void hideDataRows(boolean hidePrompts) {
        voteButtonsPanel.setVisible(false);
        Element resultDataRow = dataHtmlPanel.getElementById("resultDataRow");
        if (resultDataRow != null) {
            resultDataRow.removeFromParent();
        }

        lbCompleteCounter.setVisible(false);
        lbCompleteCounterCaption.setVisible(false);

        lbInnovativeCounter.setVisible(false);
        lbInnovativeCounterCaption.setVisible(false);

        lbCompetitiveMinCounterCaption.setVisible(false);
        lbCompetitiveMinCounter.setVisible(false);
        lbCompetitiveMaxCounterCaption.setVisible(false);
        lbCompetitiveMaxCounter.setVisible(false);
        lbCompetitiveCounterCaption.setVisible(false);
        lbCommercePotentialMinCounterCaption.setVisible(false);
        lbCommercePotentialMinCounter.setVisible(false);
        lbCommercePotentialMaxCounterCaption.setVisible(false);
        lbCommercePotentialMaxCounter.setVisible(false);
        lbCommercePotentialCounterCaption.setVisible(false);
        lbImplementMinCounterCaption.setVisible(false);
        lbImplementMinCounter.setVisible(false);
        lbImplementMaxCounterCaption.setVisible(false);
        lbImplementMaxCounter.setVisible(false);
        lbImplementCounterCaption.setVisible(false);
        lbExpertiseMinCounterCaption.setVisible(false);
        lbExpertiseMinCounter.setVisible(false);
        lbExpertiseMaxCounterCaption.setVisible(false);
        lbExpertiseMaxCounter.setVisible(false);
        lbExpertiseCounterCaption.setVisible(false);
        lbInternationalExperienceMinCounterCaption.setVisible(false);
        lbInternationalExperienceMinCounter.setVisible(false);
        lbInternationalExperienceMaxCounterCaption.setVisible(false);
        lbInternationalExperienceMaxCounter.setVisible(false);
        lbInternationalExperienceCounterCaption.setVisible(false);
        lbEnoughDocumentsMinCounterCaption.setVisible(false);
        lbEnoughDocumentsMinCounter.setVisible(false);
        lbEnoughDocumentsMaxCounterCaption.setVisible(false);
        lbEnoughDocumentsMaxCounter.setVisible(false);
        lbEnoughDocumentsCounterCaption.setVisible(false);

        if (hidePrompts) {
            hideRevisionRowGroup("dataRow");
            hideRevisionRowGroup("r2DataRow");
        }
    }

    private void hideRevisionRowGroup(String idPrefix) {
        for (int i = 1; ; ++i) {
            Element dataRow = dataHtmlPanel.getElementById(idPrefix + i);
            if (dataRow == null) break;
            dataRow.removeFromParent();
        }
    }

    private void hideRowsAccordingToRevision(VoteInfo vi) {
        if (VOTE_REVISION_1.equals(vi.revision.trim())) {
            hideRevisionRowGroup("r2DataRow");
        } else if (VOTE_REVISION_2.equals(vi.revision.trim())) {
            hideRevisionRowGroup("dataRow");
        } else {
            voteResultSpan.setInnerText("");
            noRevisionPanel.setVisible(true);
            hideDataRows(true);
        }
    }

    private void setupHandlers() {
//        LimitedTextHandler innovativeLimitedHandler = new LimitedTextHandler(taInnovativeComment, lbInnovativeCounter, commentMinLength, lb);
//        LimitedTextHandler completeLimitedHandler = new LimitedTextHandler(taCompleteComment, lbCompleteCounter, commentMinLength);
        LimitedTextHandler competitiveLimitedHandler = new LimitedTextHandler(taCompetitiveComment, lbCompetitiveMinCounter, lbCompetitiveMaxCounter);
        LimitedTextHandler commercePotentialLimitedHandler = new LimitedTextHandler(taCommercePotentialComment, lbCommercePotentialMinCounter, lbCommercePotentialMaxCounter);
        LimitedTextHandler implementLimitedHandler = new LimitedTextHandler(taImplementComment, lbImplementMinCounter, lbImplementMaxCounter);
        LimitedTextHandler expertiseLimitedHandler = new LimitedTextHandler(taExpertiseComment, lbExpertiseMinCounter, lbExpertiseMaxCounter);
        LimitedTextHandler internationalExperienceLimitedHandler = new LimitedTextHandler(taInternationalExperienceComment, lbInternationalExperienceMinCounter, lbInternationalExperienceMaxCounter);
        LimitedTextHandler enoughDocumentsLimitedHandler = new LimitedTextHandler(taEnoughDocumentsComment, lbEnoughDocumentsMinCounter, lbEnoughDocumentsMaxCounter);

//        taInnovativeComment.addValueChangeHandler(innovativeLimitedHandler);
//        taInnovativeComment.addKeyboardListener(innovativeLimitedHandler);
//        taCompleteComment.addValueChangeHandler(completeLimitedHandler);
//        taCompleteComment.addKeyboardListener(completeLimitedHandler);
        taCompetitiveComment.addValueChangeHandler(competitiveLimitedHandler);
        taCompetitiveComment.addKeyboardListener(competitiveLimitedHandler);
        taCommercePotentialComment.addValueChangeHandler(commercePotentialLimitedHandler);
        taCommercePotentialComment.addKeyboardListener(commercePotentialLimitedHandler);
        taImplementComment.addValueChangeHandler(implementLimitedHandler);
        taImplementComment.addKeyboardListener(implementLimitedHandler);
        taExpertiseComment.addValueChangeHandler(expertiseLimitedHandler);
        taExpertiseComment.addKeyboardListener(expertiseLimitedHandler);
        taInternationalExperienceComment.addValueChangeHandler(internationalExperienceLimitedHandler);
        taInternationalExperienceComment.addKeyboardListener(internationalExperienceLimitedHandler);
        taEnoughDocumentsComment.addValueChangeHandler(enoughDocumentsLimitedHandler);
        taEnoughDocumentsComment.addKeyboardListener(enoughDocumentsLimitedHandler);

        logoutLink.addClickHandler(new VoteHandler(REFUSED, false));
        bVote.addClickHandler(new VoteHandler(VOTED, true));
        bRefused.addClickHandler(new VoteHandler(REFUSED, true));
        bConnected.addClickHandler(new VoteHandler(CONNECTED, true));
        connectedQuestionLink.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                new MessageDialogBox(true, false, messages.connectedQuestion(), new HTML(messages.connectedInfo())).center();
            }
        });
    }

    private class MessageDialogBox extends DialogBox {
        private MessageDialogBox(boolean autoHide, boolean modal, String title, HTML message) {
            super(autoHide, modal);
            setTitle(title);
            setStyleName(cssBundle.css().dialog());
            VerticalPanel contents = new VerticalPanel();
            contents.add(message);

            if (!autoHide) {
                Button closeButton = new Button(messages.closeDialogButton(), new  ClickHandler() {
                    public void onClick(ClickEvent event)
                    {
                        hide();
                    }
                });
                contents.add(closeButton);
                contents.setCellHorizontalAlignment(closeButton, HasAlignment.ALIGN_CENTER);
            }

            setWidget(contents);
        }
    }

    private void addListBoxBooleanItems(ListBox listBox, int defaultValue) {
        listBox.addItem("");
        listBox.addItem(baseMessages.yes());
        listBox.addItem(baseMessages.no());
        listBox.setItemSelected(defaultValue, true);
    }

    public abstract boolean onVoted(String voteResult, boolean confirm);

    private class VoteHandler implements ClickHandler {
        private final String voteResult;
        private final boolean confirm;

        public VoteHandler(String voteResult, boolean confirm) {
            this.voteResult = voteResult;
            this.confirm = confirm;
        }

        @Override
        public void onClick(ClickEvent event) {
            if (onVoted(voteResult, confirm)) {
                voteResultSpan.setInnerText("");
                loadingPanel.setVisible(true);
                hideDataRows(true);
            }
        }
    }

    private static class LimitedTextHandler implements ValueChangeHandler<String>, KeyboardListener {
        private final TextArea taText;
        private final Label lbMinCounter;
        private final Label lbMaxCounter;

        public LimitedTextHandler(TextArea taText, Label lbMinCounter, Label lbMaxCounter) {
            this.taText = taText;
            this.lbMinCounter = lbMinCounter;
            this.lbMaxCounter = lbMaxCounter;
        }

        @Override
        public void onValueChange(ValueChangeEvent<String> stringValueChangeEvent) {
            updateCounter();
        }

        @Override
        public void onKeyDown(Widget sender, char keyCode, int modifiers) {
            updateCounter();
        }

        @Override
        public void onKeyPress(Widget sender, char keyCode, int modifiers) {
            updateCounter();
        }

        @Override
        public void onKeyUp(Widget sender, char keyCode, int modifiers) {
            updateCounter();
        }

        private void updateCounter() {
            String text = taText.getValue().trim();
            if (text == null) {
                text = "";
            }

            if (text.length() >= commentMinLength)
                lbMinCounter.setStyleName(cssBundle.css().commentBox());
            else
                lbMinCounter.setStyleName(cssBundle.css().warningCommentBox());

            if (text.length() <= commentMaxLength)
                lbMaxCounter.setStyleName(cssBundle.css().commentBox());
            else
                lbMaxCounter.setStyleName(cssBundle.css().warningCommentBox());

            lbMinCounter.setText("" + Math.max(0, commentMinLength - text.length()));
            lbMaxCounter.setText("" + Math.max(0, commentMaxLength - text.length()));
        }
    }
}