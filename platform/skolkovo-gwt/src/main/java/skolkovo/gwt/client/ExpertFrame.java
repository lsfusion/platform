package skolkovo.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import skolkovo.gwt.shared.GwtVoteInfo;

public class ExpertFrame implements EntryPoint {
    private Button button;
    private Label projectLabel;

    public void onModuleLoad() {
        GwtVoteInfo voteInfo = new GwtVoteInfo();
        voteInfo.expertName = "Some guy";
        voteInfo.cluster = "Медицинские технологии в области разработки оборудования, лекарственных средств";


        VerticalPanel contentPane = new VerticalPanel();

        TextArea caption = new TextArea();
        caption.setText("Вы получили следующие материалы по проекту:\n" +
                        "  1. Анкета проекта\n" +
                        "  2. Резюме проекта\n" +
                        "  3. Резюме иностранного специалиста, участвующего и/или планирующего участвовать в проекте (опционально)\n" +
                        "  4. Техническое описание продукта/технологии (опционально)\n" +
                        "\n" +
                        "Пожалуйста, направьте заполненный бюллетень по адресу experts@i-gorod.com");
//        caption.setEnabled(false);
        caption.setReadOnly(true);
        caption.setSize("100%", "");
        caption.setVisibleLines(8);

        CheckBox cbInterest = new CheckBox("Являетесь ли Вы заинтересованным лицом по отношению к заявителю проекта?");
        CheckBox cbClusterSuitable = new CheckBox("Проект соответствует направлению \"" + voteInfo.cluster + "\"");
        CheckBox cbConcurrentProject = new CheckBox("Проект предполагает разработку и/или коммерциализацию уникальных и/или обладающих конкурентными преимуществами перед мировыми аналогами продуктов и/или технологий?");

        Label lbConcurrentLabel = new Label("Приведите обоснование Вашего ответа (800-1000 символов):");
        TextArea taConcurrentDescription = new TextArea();
        taConcurrentDescription.setSize("100%", "100");

        CheckBox cbForeignSpecialistInvolved = new CheckBox("Проект предполагает участие иностранного (не являющегося гражданином Российской Федерации) специалиста, который имеет значительный авторитет в инвестиционной и/или исследовательской среде");

        button = new Button("Update projects");
        button.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
            }
        });


        contentPane.setSpacing(10);
        contentPane.add(caption);
        contentPane.add(cbInterest);
        contentPane.add(cbClusterSuitable);
        contentPane.add(cbConcurrentProject);
        contentPane.add(lbConcurrentLabel);
        contentPane.add(taConcurrentDescription);
        contentPane.add(cbForeignSpecialistInvolved);

        RootPanel.get().add(contentPane);
    }

    private void fillProjectsList() {
//        projectsList.clear();
//        projectsList.addItem("...loading...");
//        ExpertService.App.getInstance().getVoteInfo(0, 0, new AsyncCallback<String[]>() {
//            public void onFailure(Throwable caught) {
//            }
//
//            public void onSuccess(String[] projects) {
//                projectsList.clear();
//                if (projects != null) {
//                    for (String project : projects) {
//                        projectsList.addItem(project);
//                    }
//                }
//            }
//        });
        ExpertService.App.getInstance().getVoteInfo("", 0, new AsyncCallback<GwtVoteInfo>() {
            public void onFailure(Throwable caught) {
                //todo:

            }

            public void onSuccess(GwtVoteInfo result) {
                projectLabel.setText(result.expertName);
            }
        });
    }
}
