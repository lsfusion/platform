package skolkovo.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import skolkovo.gwt.shared.GwtVoteInfo;

public class ExpertFrame implements EntryPoint {

    public void onModuleLoad() {
        update();
    }

    private void update() {

        ExpertService.App.getInstance().getVoteInfo("admin", 0, new AsyncCallback<GwtVoteInfo>() {
            public void onFailure(Throwable caught) {
                //todo: change it
                onSuccess(null);
            }

            public void onSuccess(GwtVoteInfo vi) {
                if (vi == null) {
                    vi = new GwtVoteInfo();
                    vi.projectClaimer = "НьюВак";
                    vi.expertName = "Some guy";
                    vi.cluster = "Медицинские технологии в области разработки оборудования, лекарственных средств";
                }

                VerticalPanel manePane = new VerticalPanel();

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
                                "Пожалуйста, заполните данный бюллетень");

                CheckBox cbInterest = new CheckBox("Являетесь ли Вы заинтересованным лицом по отношению к заявителю проекта?");
                CheckBox cbClusterSuitable = new CheckBox("Проект соответствует направлению \"" + vi.cluster + "\"");
                CheckBox cbConcurrentProject = new CheckBox("Проект предполагает разработку и/или коммерциализацию уникальных и/или обладающих конкурентными преимуществами перед мировыми аналогами продуктов и/или технологий?");

                Label lbConcurrentLabel = new Label("Приведите обоснование Вашего ответа (800-1000 символов):");
                TextArea taConcurrentDescription = new TextArea();
                taConcurrentDescription.setSize("100%", "");
                taConcurrentDescription.setVisibleLines(8);

                CheckBox cbForeignSpecialistInvolved = new CheckBox("Проект предполагает участие иностранного (не являющегося гражданином Российской Федерации) специалиста, который имеет значительный авторитет в инвестиционной и/или исследовательской среде");

                Label lbCompetence = new Label("Оцените Вашу компетенцию по теме проекта (от 1 до 5 баллов)");
                ListBox bxCompetence = new ListBox();
                bxCompetence.setWidth("100%");
                for (int i = 1; i <= 5; ++i) {
                    bxCompetence.addItem("" + i);
                }

                Label lbCompleteness = new Label("Оцените достаточность представленных материалов для оценки проекта (от 1 до 5 баллов)");
                ListBox bxCompleteness = new ListBox();
                bxCompleteness.setWidth("100%");
                for (int i = 1; i <= 5; ++i) {
                    bxCompleteness.addItem("" + i);
                }

                Label lbCompletnessDescription = new Label("Приведите обоснование Вашей оценки (200-300 символов):");
                TextArea taCompletnessDescription = new TextArea();
                taCompletnessDescription.setSize("100%", "");
                taCompletnessDescription.setVisibleLines(8);

                Button bVote = new Button("Оценить");
                bVote.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        update();
                    }
                });

                Button bCancel = new Button("Отказаться от оценки");
                bCancel.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        update();
                    }
                });

                Button bAffilate = new Button("Я являюсь заинтересованным лицом");
                bAffilate.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        update();
                    }
                });

                manePane.setWidth("600");
                manePane.setSpacing(10);
                manePane.add(caption);
                manePane.add(createVerticalSpacer(20));
                manePane.add(cbInterest);
                manePane.add(cbClusterSuitable);
                manePane.add(cbConcurrentProject);
                manePane.add(lbConcurrentLabel);
                manePane.add(taConcurrentDescription);
                manePane.add(cbForeignSpecialistInvolved);
                manePane.add(lbCompetence);
                manePane.add(bxCompetence);
                manePane.add(lbCompleteness);
                manePane.add(bxCompleteness);
                manePane.add(lbCompletnessDescription);
                manePane.add(taCompletnessDescription);
                manePane.add(bVote);
                manePane.add(bCancel);
                manePane.add(bAffilate);

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
        });
    }

    private Widget createVerticalSpacer(int height) {
        VerticalPanel spacer = new VerticalPanel();
        spacer.setHeight(Integer.toString(height));
        spacer.setWidth("100%");
        return spacer;
    }
}
