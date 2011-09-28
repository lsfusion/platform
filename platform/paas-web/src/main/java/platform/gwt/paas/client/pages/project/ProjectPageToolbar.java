package platform.gwt.paas.client.pages.project;

import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import platform.gwt.paas.client.widgets.ToolbarWithUIHandlers;

public class ProjectPageToolbar extends ToolbarWithUIHandlers<ProjectPageUIHandlers> {

    public ProjectPageToolbar() {
        addHomeButton();
        addSeparator();
        addToolStripButton("module_add.png", "Add module", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                uiHandlers.addNewModuleButtonClicked();
            }
        });

        addToolStripButton("save.png", "Save all", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                uiHandlers.saveAllButtonClicked();
            }
        });

        addToolStripButton("refresh.png", "Refresh", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                uiHandlers.refreshButtonClicked(true);
            }
        });

        addSeparator();

        addToolStripButton("configuration.png", "Run configurations", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                uiHandlers.configurationButtonClicked();
            }
        });

        addFill();

        addConsoleButton();

        addLogoffButton();
    }
}
