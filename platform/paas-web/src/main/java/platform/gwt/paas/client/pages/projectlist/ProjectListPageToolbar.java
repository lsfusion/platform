package platform.gwt.paas.client.pages.projectlist;

import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import platform.gwt.paas.client.widgets.ToolbarWithUIHandlers;

public class ProjectListPageToolbar extends ToolbarWithUIHandlers<ProjectsListPageUIHandlers> {
    public ProjectListPageToolbar() {
        addToolStripButton("add.png", "Add new project", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                uiHandlers.addNewProject();
            }
        });

        addToolStripButton("refresh.png", "Refresh", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                uiHandlers.refreshButtonClicked();
            }
        });

        addFill();

        addConsoleButton();

        addLogoutButton();
    }
}
