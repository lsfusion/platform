package skolkovo.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import skolkovo.gwt.shared.GwtVoteInfo;

public class ExpertFrame implements EntryPoint {
    private Button button;
    private Label projectLabel;

    public void onModuleLoad() {
        button = new Button("Update projects");
        button.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
            }
        });

        projectLabel = new Label();

        RootPanel.get("buttonHolder").add(button);
        RootPanel.get("projectInfoHolder").add(projectLabel);

        fillProjectsList();
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
        ExpertService.App.getInstance().getVoteInfo(0, 0, new AsyncCallback<GwtVoteInfo>() {
            public void onFailure(Throwable caught) {
                //todo:

            }

            public void onSuccess(GwtVoteInfo result) {
                projectLabel.setText(result.expertName);
            }
        });
    }
}
