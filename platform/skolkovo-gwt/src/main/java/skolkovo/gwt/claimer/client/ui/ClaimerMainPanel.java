package skolkovo.gwt.claimer.client.ui;

import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import platform.gwt.sgwtbase.client.ui.ToolStripPanel;
import skolkovo.gwt.claimer.client.ClaimerMessages;

public class ClaimerMainPanel extends VLayout {
    private static ClaimerMessages messages = ClaimerMessages.Instance.get();

    private ToolStripPanel toolbar;
    private ListGrid projectsGrid;
    private HLayout projectInfoPane;

    public ClaimerMainPanel() {
        createToolbar();

        createProjectsGrid();

        configureInfoPane();

        configureLayout();
    }

    private void createToolbar() {
        toolbar = new ToolStripPanel(messages.title());
    }

    private void createProjectsGrid() {
        projectsGrid = new ProjectsListGrid();
    }

    private void configureInfoPane() {
        ProjectApplicationForm infoForm = new ProjectApplicationForm();

        projectInfoPane = new HLayout();
        projectInfoPane.setLayoutMargin(5);
        projectInfoPane.setWidth100();
        projectInfoPane.setHeight100();
        projectInfoPane.addMember(infoForm);
        projectInfoPane.setOverflow(Overflow.AUTO);
    }

    private void configureLayout() {
        VLayout topPane = new VLayout();
        topPane.setAutoHeight();
        topPane.addMember(toolbar);

        HLayout leftPane = new HLayout();
        leftPane.setLayoutMargin(5);
        leftPane.setWidth(300);
        leftPane.setShowResizeBar(true);
        leftPane.addMember(projectsGrid);

        HLayout bottomPane = new HLayout();
        bottomPane.addMember(leftPane);
        bottomPane.addMember(projectInfoPane);

        setWidth100();
        setHeight100();
        addMember(topPane);
        addMember(bottomPane);
    }
}
