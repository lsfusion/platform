package skolkovo.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import skolkovo.gwt.shared.GwtVoteInfo;

public class ExpertFrame implements EntryPoint {

    private CheckBox cbInCluster;
    private CheckBox cbInnovative;
    private Label lbInnovative;
    private TextArea taInnovativeComment;
    private CheckBox cbForeign;
    private Label lbCompetent;
    private ListBox bxCompetent;
    private Label lbComplete;
    private ListBox bxComplete;
    private Label lbCompleteComment;
    private TextArea taCompleteComment;

    public void onModuleLoad() {
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
            showErrorPage();
            return;
        }

        ExpertService.App.getInstance().getVoteInfo(voteId, new AsyncCallback<GwtVoteInfo>() {
            public void onFailure(Throwable caught) {
                showErrorPage();
            }

            public void onSuccess(GwtVoteInfo vi) {
                if (vi == null) {
                    showErrorPage();
                    return;
                }

                HTMLPanel caption = new HTMLPanel(
                        "<h4>БЮЛЛЕТЕНЬ ОЦЕНКИ ПРОЕКТА</h4>" +
                        "Эксперт: <i>" + vi.expertName + "</i><br/>" +
                        "<br/>" +
                        "Организация-заявитель: <i>" + vi.projectClaimer + "</i><br/>" +
                        "<br/>" +
                        "Вы получили следующие материалы по проекту:<br/>" +
                        "&nbsp;&nbsp;1. Анкета проекта<br/>" +
                        "&nbsp;&nbsp;2. Резюме проекта<br/>" +
                        "&nbsp;&nbsp;3. Резюме иностранного специалиста, участвующего и/или планирующего участвовать в проекте (опционально)<br/>" +
                        "&nbsp;&nbsp;4. Техническое описание продукта/технологии (опционально)<br/>" +
                        "<br/>" +
                        (vi.voteDone ? "" : "Пожалуйста, заполните данный бюллетень") +
                        "<h5>" +
                        ("voted".equals(vi.voteResult)
                             ? "Вы уже оценили эту заявку."
                             : "refused".equals(vi.voteResult)
                                 ? "Вы отказались от оценки этой заявки."
                                 : "connected".equals(vi.voteResult)
                                     ? "Вы являетесь заинтерисованным лицом для этой заявки."
                                     : ""
                        ) +
                        "</h5>"
                );

                VerticalPanel manePane = new VerticalPanel();
                manePane.setWidth("1024");
                manePane.setSpacing(10);
                manePane.add(caption);

                if (!vi.voteDone || "voted".equals(vi.voteResult)) {
                    manePane.add(createVerticalSpacer(20));
                    cbInCluster = new CheckBox("Проект соответствует направлению \"" + vi.projectCluster + "\"");
                    cbInCluster.setValue(vi.inCluster);
                    cbInCluster.setEnabled(!vi.voteDone);

                    cbInnovative = new CheckBox("Проект предполагает разработку и/или коммерциализацию уникальных и/или обладающих конкурентными преимуществами перед мировыми аналогами продуктов и/или технологий?");
                    cbInnovative.setValue(vi.innovative);
                    cbInnovative.setEnabled(!vi.voteDone);

                    lbInnovative = new Label("Приведите обоснование Вашего ответа (800-1000 символов):");
                    taInnovativeComment = new TextArea();
                    taInnovativeComment.setSize("100%", "");
                    taInnovativeComment.setVisibleLines(8);
                    taInnovativeComment.setText(vi.innovativeComment);
                    taInnovativeComment.setEnabled(!vi.voteDone);

                    cbForeign = new CheckBox("Проект предполагает участие иностранного (не являющегося гражданином Российской Федерации) специалиста, который имеет значительный авторитет в инвестиционной и/или исследовательской среде");
                    cbForeign.setEnabled(!vi.voteDone);

                    lbCompetent = new Label("Оцените Вашу компетенцию по теме проекта (от 1 до 5 баллов)");
                    bxCompetent = new ListBox();
                    bxCompetent.setWidth("10%");
                    for (int i = 1; i <= 5; ++i) {
                        bxCompetent.addItem("" + i);
                    }
                    bxCompetent.setItemSelected(vi.competent - 1, true);
                    bxCompetent.setEnabled(!vi.voteDone);

                    lbComplete = new Label("Оцените достаточность представленных материалов для оценки проекта (от 1 до 5 баллов)");
                    bxComplete = new ListBox();
                    bxComplete.setWidth("10%");
                    for (int i = 1; i <= 5; ++i) {
                        bxComplete.addItem("" + i);
                    }
                    bxComplete.setItemSelected(vi.complete - 1, true);
                    bxComplete.setEnabled(!vi.voteDone);

                    lbCompleteComment = new Label("Приведите обоснование Вашей оценки (200-300 символов):");
                    taCompleteComment = new TextArea();
                    taCompleteComment.setSize("100%", "");
                    taCompleteComment.setVisibleLines(8);
                    taCompleteComment.setText(vi.completeComment);
                    taCompleteComment.setEnabled(!vi.voteDone);

                    manePane.add(cbInCluster);
                    manePane.add(cbInnovative);
                    manePane.add(lbInnovative);
                    manePane.add(taInnovativeComment);
                    manePane.add(cbForeign);
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

    private Panel createButtonsPane() {
        Button bVote = new Button("Оценить");
        bVote.addClickHandler(new VoteClickHandler("voted"));

        Button bRefused = new Button("Отказаться от оценки");
        bRefused.addClickHandler(new VoteClickHandler("refused"));

        Button bConnected = new Button("Я являюсь заинтересованным лицом");
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

    private void showErrorPage() {
        VerticalPanel manePane = new VerticalPanel();

        HTMLPanel caption = new HTMLPanel(
                        "<h5>Произошла ошибка при обработке запроса.</h5>");

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
            RootPanel.get().clear();

            GwtVoteInfo vi = new GwtVoteInfo();

            vi.voteResult = voteResult;

            vi.inCluster = cbInCluster.getValue();
            vi.innovative = cbInnovative.getValue();
            vi.innovativeComment = taInnovativeComment.getText();
            vi.foreign = cbForeign.getValue();
            vi.competent = bxCompetent.getSelectedIndex() + 1;
            vi.complete = bxComplete.getSelectedIndex() + 1;
            vi.completeComment = taCompleteComment.getText();

            int voteId = getVoteId();
            if (voteId == -1) {
                showErrorPage();
                return;
            }

            ExpertService.App.getInstance().setVoteInfo(vi, getVoteId(), new AsyncCallback<Void>() {
                public void onFailure(Throwable caught) {
                    showErrorPage();
                }

                public void onSuccess(Void result) {
                    update();
                }
            });
        }
    }
}
