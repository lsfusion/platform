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
    private Label lbInCluster;
    private ListBox bxInCluster;
    private Label lbInnovative;
    private ListBox bxInnovative;
    private Label lbInnovativeComment;
    private TextArea taInnovativeComment;
    private Label lbForeign;
    private ListBox bxForeign;
    private Label lbCompetent;
    private ListBox bxCompetent;
    private Label lbComplete;
    private ListBox bxComplete;
    private Label lbCompleteComment;
    private TextArea taCompleteComment;

    public void onModuleLoad() {
        Window.setTitle(getMessages().title());
        update();
    }


    public int getVoteId() {
        try {
            return Integer.parseInt(Window.Location.getParameter("voteId"));
        } catch (Exception nfe) {
            return -1;
        }
    }

    private void update() {
        int voteId = getVoteId();
        if (voteId == -1) {
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
                HTMLPanel caption = new HTMLPanel(
                        getMessages().caption(
                                vi.expertName,
                                vi.projectClaimer,
                                vi.voteDone ? "" : getMessages().pleasePrompt(),
                                "voted".equals(vi.voteResult)
                                ? getMessages().votedPrompt()
                                : "refused".equals(vi.voteResult)
                                  ? getMessages().refusedPrompt()
                                  : "connected".equals(vi.voteResult)
                                    ? getMessages().connectedPrompt()
                                    : "",
                                DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_SHORT).format(voteDate)
                        ));

                VerticalPanel manePane = new VerticalPanel();
                manePane.setWidth("1024");
                manePane.setSpacing(10);
                manePane.add(createLocalesPanel());
                manePane.add(caption);

                if (!vi.voteDone || "voted".equals(vi.voteResult)) {
                    if (!vi.voteDone) {
                        manePane.add(createVerticalSpacer(20));
                    }

                    lbInCluster = new Label(getMessages().lbInCluster(vi.projectCluster));

                    bxInCluster = new ListBox();
                    bxInCluster.setWidth("10%");
                    addListBoxBooleanItems(bxInCluster, vi.voteDone ? (vi.inCluster ? 1 : 2) : 0);
                    bxInCluster.setEnabled(!vi.voteDone);

                    lbInnovative = new Label(getMessages().lbInnovative());
                    bxInnovative = new ListBox();
                    bxInnovative.setWidth("10%");
                    addListBoxBooleanItems(bxInnovative, vi.voteDone ? (vi.innovative ? 1 : 2) : 0);
                    bxInnovative.setEnabled(!vi.voteDone);

                    lbInnovativeComment = new Label(getMessages().lbInnovativeComment());
                    taInnovativeComment = new TextArea();
                    taInnovativeComment.setSize("100%", "");
                    taInnovativeComment.setVisibleLines(8);
                    taInnovativeComment.setText(vi.innovativeComment);
                    taInnovativeComment.setEnabled(!vi.voteDone);

                    lbForeign = new Label(getMessages().lbForeign());
                    bxForeign = new ListBox();
                    bxForeign.setWidth("10%");
                    addListBoxBooleanItems(bxForeign, vi.voteDone ? (vi.foreign ? 1 : 2) : 0);
                    bxForeign.setEnabled(!vi.voteDone);

                    lbCompetent = new Label(getMessages().lbCompetent());
                    bxCompetent = new ListBox();
                    bxCompetent.setWidth("10%");
                    for (int i = 1; i <= 5; ++i) {
                        bxCompetent.addItem("" + i);
                    }
                    bxCompetent.setItemSelected(vi.competent - 1, true);
                    bxCompetent.setEnabled(!vi.voteDone);

                    lbComplete = new Label(getMessages().lbComplete());
                    bxComplete = new ListBox();
                    bxComplete.setWidth("10%");
                    for (int i = 1; i <= 5; ++i) {
                        bxComplete.addItem("" + i);
                    }
                    bxComplete.setItemSelected(vi.complete - 1, true);
                    bxComplete.setEnabled(!vi.voteDone);

                    lbCompleteComment = new Label(getMessages().lbCompleteComment());
                    taCompleteComment = new TextArea();
                    taCompleteComment.setSize("100%", "");
                    taCompleteComment.setVisibleLines(8);
                    taCompleteComment.setText(vi.completeComment);
                    taCompleteComment.setEnabled(!vi.voteDone);

                    manePane.add(lbInCluster);
                    manePane.add(bxInCluster);
                    manePane.add(lbInnovative);
                    manePane.add(bxInnovative);
                    manePane.add(lbInnovativeComment);
                    manePane.add(taInnovativeComment);
                    manePane.add(lbForeign);
                    manePane.add(bxForeign);
                    manePane.add(lbCompetent);
                    manePane.add(bxCompetent);
                    manePane.add(lbComplete);
                    manePane.add(bxComplete);
                    manePane.add(lbCompleteComment);
                    manePane.add(taCompleteComment);
                    if (!vi.voteDone) {
                        manePane.add(createButtonsPane());
                    }
                }

                setManePane(manePane);
            }
        });
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
        listBox.addItem("Да");
        listBox.addItem("Нет");
        listBox.setItemSelected(defaultValue, true);
    }

    private Panel createButtonsPane() {
        Button bVote = new Button(getMessages().btnVote());
        bVote.addClickHandler(new VoteClickHandler("voted"));

        Button bRefused = new Button(getMessages().btnRefused());
        bRefused.addClickHandler(new VoteClickHandler("refused"));

        Button bConnected = new Button(getMessages().btnConnected());
        bConnected.addClickHandler(new VoteClickHandler("connected"));

        HorizontalPanel btnPanel = new HorizontalPanel();
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

        private VoteClickHandler(String voteResult) {
            this.voteResult = voteResult;
        }

        public void onClick(ClickEvent event) {
            if ("voted".equals(voteResult) &&
                (bxInCluster.getSelectedIndex() == 0 ||
                 bxInnovative.getSelectedIndex() == 0 ||
                 bxForeign.getSelectedIndex() == 0)) {
                Window.alert(getMessages().incompletePrompt());
                return;
            }

            if (!Window.confirm(getMessages().confirmPrompt())) {
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

            int voteId = getVoteId();
            if (voteId == -1) {
                showErrorPage(null);
                return;
            }

            ExpertService.App.getInstance().setVoteInfo(vi, getVoteId(), new AsyncCallback<Void>() {
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
