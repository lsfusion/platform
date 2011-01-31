package skolkovo.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import skolkovo.gwt.shared.GwtVoteInfo;
import skolkovo.gwt.shared.MessageException;

import java.util.Date;

public class ExpertFrame implements EntryPoint {
    private ListBox bxInCluster;
    private ListBox bxInnovative;
    private TextArea taInnovativeComment;
    private ListBox bxForeign;
    private ListBox bxCompetent;
    private ListBox bxComplete;
    private TextArea taCompleteComment;

    public void onModuleLoad() {
        Window.setTitle(getMessages().title());
        update();
    }


    public String getVoteId() {
        try {
            return Window.Location.getParameter("voteId");
        } catch (Exception nfe) {
            return null;
        }
    }

    private void update() {

        String voteId = getVoteId();
        if (voteId == null) {
            showErrorPage(null);
            return;
        }

        ExpertService.App.getInstance().getVoteInfo(voteId, new AsyncCallback<GwtVoteInfo>() {
            public void onFailure(Throwable caught) {
                showErrorPage(caught);
            }

            public void onSuccess(GwtVoteInfo vi) {
                if (vi == null) {
                    showErrorPage(null);
                    return;
                }

                Date voteDate = vi.date == null ? new Date() : vi.date;
                HTMLPanel headerCaption = new HTMLPanel(
                        getMessages().headerCaption(
                                vi.projectName,
                                vi.projectClaimer,
                                vi.expertName,
                                DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_SHORT).format(voteDate)
                        ));

                if (!vi.voteDone) {
                    //вставляем ссылку для отказа...
                    Hyperlink logoutLink = new Hyperlink(getMessages().here(), "");
                    logoutLink.addClickHandler(new VoteClickHandler("refused", false));
                    headerCaption.add(logoutLink, "logoutLink");
                } else {
                    //...или убираем примечание
                    headerCaption.addAndReplaceElement(new Label(), headerCaption.getElementById("logoutNotice"));
                }

                VerticalPanel manePane = new VerticalPanel();
                manePane.setWidth("800");
                manePane.setSpacing(5);
                manePane.add(createLocalesPanel());
                manePane.add(headerCaption);
                manePane.add(createVerticalSpacer(5));
                manePane.add(
                        new HTMLPanel(
                                !vi.voteDone ? getMessages().pleasePrompt()
                                             : "<b>" +
                                               ("voted".equals(vi.voteResult)
                                                ? getMessages().votedPrompt()
                                                : "refused".equals(vi.voteResult)
                                                  ? getMessages().refusedPrompt()
                                                  : "connected".equals(vi.voteResult)
                                                    ? getMessages().connectedPrompt()
                                                    : "") +
                                               "</b>"));


                if (!vi.voteDone || "voted".equals(vi.voteResult)) {
                    bxInCluster = new ListBox();
                    addListBoxBooleanItems(bxInCluster, vi.voteDone ? (vi.inCluster ? 1 : 2) : 0);
                    bxInCluster.setEnabled(!vi.voteDone);

                    Widget wInCluster = createCaptionedListBox(getMessages().lbInCluster(vi.projectCluster), bxInCluster);

                    bxInnovative = new ListBox();
                    addListBoxBooleanItems(bxInnovative, vi.voteDone ? (vi.innovative ? 1 : 2) : 0);
                    bxInnovative.setEnabled(!vi.voteDone);

                    Widget wInnovative = createCaptionedListBox(getMessages().lbInnovative(), bxInnovative);

                    taInnovativeComment = new TextArea();
                    taInnovativeComment.setSize("100%", "");
                    taInnovativeComment.setVisibleLines(8);
                    taInnovativeComment.setText(vi.innovativeComment);
                    taInnovativeComment.setEnabled(!vi.voteDone);

                    Widget wInnovativeComment = createListBoxWithCaptionedTextArea(wInnovative, getMessages().lbInnovativeComment(), taInnovativeComment);

                    bxForeign = new ListBox();
                    addListBoxBooleanItems(bxForeign, vi.voteDone ? (vi.foreign ? 1 : 2) : 0);
                    bxForeign.setEnabled(!vi.voteDone);

                    Widget wForeign = createCaptionedListBox(getMessages().lbForeign(), bxForeign);

                    bxCompetent = new ListBox();
                    for (int i = 1; i <= 5; ++i) {
                        bxCompetent.addItem("" + i);
                    }
                    bxCompetent.setItemSelected(vi.competent - 1, true);
                    bxCompetent.setEnabled(!vi.voteDone);

                    Widget wCompetent = createCaptionedListBox(getMessages().lbCompetent(), bxCompetent);

                    bxComplete = new ListBox();
                    for (int i = 1; i <= 5; ++i) {
                        bxComplete.addItem("" + i);
                    }
                    bxComplete.setItemSelected(vi.complete - 1, true);
                    bxComplete.setEnabled(!vi.voteDone);

                    Widget wComplete = createCaptionedListBox(getMessages().lbComplete(), bxComplete);

                    taCompleteComment = new TextArea();
                    taCompleteComment.setSize("100%", "");
                    taCompleteComment.setVisibleLines(8);
                    taCompleteComment.setText(vi.completeComment);
                    taCompleteComment.setEnabled(!vi.voteDone);

                    Widget wCompleteComment = createListBoxWithCaptionedTextArea(wComplete, getMessages().lbCompleteComment(), taCompleteComment);

                    manePane.add(createNumberPanel(1, wInCluster, false));
                    manePane.add(createNumberPanel(2, wInnovativeComment, false));
                    manePane.add(createNumberPanel(3, wForeign, false));
                    manePane.add(createNumberPanel(4, wCompetent, false));
                    manePane.add(createNumberPanel(5, wCompleteComment, false));

                    if (!vi.voteDone) {
                        HTMLPanel footerCaption = new HTMLPanel(getMessages().footerCaption());
                        manePane.add(createVerticalSpacer(5));
                        manePane.add(footerCaption);
                        manePane.add(createButtonsPane());
                    }
                }

                setManePane(manePane);
            }
        });
    }

    private Widget createNumberPanel(int n, Widget widget, boolean addSpacing) {
        VerticalPanel pane = new VerticalPanel();
        pane.setWidth("100%");

        if (addSpacing) {
            pane.add(createVerticalSpacer(10));
        }

        pane.add(new Label("" + n + "."));
        pane.add(widget);

        return pane;
    }

    private Widget createCaptionedListBox(String caption, ListBox listBox) {
        Label lbCaption = new Label(caption);

        HorizontalPanel pane = new HorizontalPanel();
        pane.setWidth("100%");
        pane.add(lbCaption);
        pane.add(listBox);

        pane.setCellWidth(lbCaption, "80%");
        pane.setCellWidth(listBox, "20%");
        lbCaption.setWidth("100%");
        listBox.setWidth("100%");

        pane.setStyleName("captionedListBox");

        return pane;
    }

    private Widget createListBoxWithCaptionedTextArea(Widget wListBox, String caption, TextArea textArea) {
        Label lbCaption = new Label(caption);

        VerticalPanel panText = new VerticalPanel();
        panText.setWidth("100%");
        panText.add(lbCaption);
        panText.add(textArea);

        lbCaption.setWidth("100%");
        textArea.setWidth("100%");

        panText.setStyleName("captionedTextArea");


        VerticalPanel pane = new VerticalPanel();
        pane.setWidth("100%");
        pane.add(wListBox);
        pane.add(panText);

        return pane;
    }

    private VerticalPanel createLocalesPanel() {
        HorizontalPanel localeBtnPanel = new HorizontalPanel();
        localeBtnPanel.setHorizontalAlignment(HorizontalPanel.ALIGN_RIGHT);
        localeBtnPanel.setSpacing(5);
        for (final String localeName : LocaleInfo.getAvailableLocaleNames()) {
            // пропускаем GWT-шную дефолтную локаль
            if ("default".equals(localeName)) {
                continue;
            }
            localeBtnPanel.add(new HTML("<a href=\"" + Window.Location.createUrlBuilder().setParameter("locale", localeName).buildString() + "\">" +
                                        "  <img src=\"images/" + localeName + ".png\"/></a>"));
        }

        VerticalPanel localesPanel = new VerticalPanel();
        localesPanel.setWidth("100%");
        localesPanel.setHorizontalAlignment(HorizontalPanel.ALIGN_RIGHT);
        localesPanel.add(localeBtnPanel);
        return localesPanel;
    }

    private ExpertFrameMessages getMessages() {
        return ExpertService.Messages.getInstance();
    }

    private void addListBoxBooleanItems(ListBox listBox, int defaultValue) {
        listBox.addItem("");
        listBox.addItem(getMessages().yes());
        listBox.addItem(getMessages().no());
        listBox.setItemSelected(defaultValue, true);
    }

    private Panel createButtonsPane() {
        Button bVote = new Button(getMessages().btnVote());
        bVote.addClickHandler(new VoteClickHandler("voted"));

        Button bRefused = new Button(getMessages().btnRefused());
        bRefused.addClickHandler(new VoteClickHandler("refused"));

        Button bConnected = new Button(getMessages().btnConnected());
        bConnected.addClickHandler(new VoteClickHandler("connected"));

        VerticalPanel btnPanel = new VerticalPanel();
        btnPanel.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
        btnPanel.setSpacing(3);
        btnPanel.add(bVote);
        btnPanel.add(bRefused);
        btnPanel.add(bConnected);

        VerticalPanel pane = new VerticalPanel();
        pane.setWidth("100%");
        pane.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
        pane.add(btnPanel);

        return pane;
    }

    private void setManePane(VerticalPanel manePane) {
        manePane.setStyleName("manePane");

        VerticalPanel contentPane = new VerticalPanel();
        contentPane.setWidth("100%");
        contentPane.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);

        VerticalPanel borderPanel = new VerticalPanel();
        borderPanel.add(manePane);
        borderPanel.setBorderWidth(1);

        contentPane.add(borderPanel);

        RootPanel.get().clear();
        RootPanel.get().add(contentPane);
    }

    private void showErrorPage(Throwable caught) {

        String message = caught instanceof MessageException
                         ? caught.getMessage()
                         : getMessages().internalServerErrorMessage();

        HTMLPanel caption = new HTMLPanel(
                "<h5>" + message + "</h5>");

        VerticalPanel manePane = new VerticalPanel();
        manePane.setWidth("1024");
        manePane.setSpacing(10);
        manePane.add(caption);

        setManePane(manePane);
    }

    private Widget createVerticalSpacer(int height) {
        VerticalPanel spacer = new VerticalPanel();
        spacer.setHeight(Integer.toString(height));
        spacer.setWidth("100%");
        return spacer;
    }

    private class VoteClickHandler implements ClickHandler {
        private final String voteResult;
        private final boolean confirm;

        private VoteClickHandler(String voteResult) {
            this(voteResult, true);
        }

        private VoteClickHandler(String voteResult, boolean confirm) {
            this.voteResult = voteResult;
            this.confirm = confirm;
        }

        public void onClick(ClickEvent event) {
            if ("voted".equals(voteResult) &&
                (bxInCluster.getSelectedIndex() == 0 ||
                 bxInnovative.getSelectedIndex() == 0 ||
                 bxForeign.getSelectedIndex() == 0)) {
                Window.alert(getMessages().incompletePrompt());
                return;
            }

            if (confirm && !Window.confirm(getMessages().confirmPrompt())) {
                return;
            }

            RootPanel.get().clear();

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

            ExpertService.App.getInstance().setVoteInfo(vi, voteId, new AsyncCallback<Void>() {
                public void onFailure(Throwable caught) {
                    showErrorPage(caught);
                }

                public void onSuccess(Void result) {
                    update();
                }
            });
        }
    }
}
