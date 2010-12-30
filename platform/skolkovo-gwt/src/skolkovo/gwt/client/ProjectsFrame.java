package skolkovo.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RootPanel;

public class ProjectsFrame implements EntryPoint {
    private ListBox projectsList;
    private Button button;
    private Label projectLabel;

    public void onModuleLoad() {
        button = new Button("Update projects");
        button.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                fillProjectsList();
                updateProjectInfo();
            }
        });

        projectsList = new ListBox();
        projectsList.setVisibleItemCount(10);
        projectsList.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                updateProjectInfo();
            }
        });

        projectLabel = new Label();

        RootPanel.get("buttonHolder").add(button);
        RootPanel.get("projectListHolder").add(projectsList);
        RootPanel.get("projectInfoHolder").add(projectLabel);

        fillProjectsList();
        updateProjectInfo();
    }

    private void updateProjectInfo() {
        int selIndex = projectsList.getSelectedIndex();
        String projectName = selIndex == -1 ? "<none>" : projectsList.getItemText(selIndex);
        projectLabel.setText(projectName);
    }

    private void fillProjectsList() {
        projectsList.clear();
        projectsList.addItem("...loading...");
        ProjectsService.App.getInstance().getProjects(new AsyncCallback<String[]>(){
            public void onFailure(Throwable caught) {}
            public void onSuccess(String[] projects) {
                projectsList.clear();
                for (String project : projects) {
                    projectsList.addItem(project);
                }
            }
        });
    }
}
