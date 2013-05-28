package platform.gwt.paas.client.pages.projectlist;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewWithUiHandlers;
import com.smartgwt.client.widgets.grid.events.RecordDoubleClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordDoubleClickHandler;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import paas.api.gwt.shared.dto.ProjectDTO;
import platform.gwt.paas.client.data.ProjectRecord;

public class ProjectsListPageView extends ViewWithUiHandlers<ProjectsListPageUIHandlers> implements ProjectsListPagePresenter.MyView {
    private final ProjectListPageToolbar toolbar;
    private ProjectsListGrid projectsGrid;

    private ProjectInfoPane projectInfoPane;

    private VLayout mainPane;

    @Inject
    public ProjectsListPageView(ProjectListPageToolbar itoolbar, ProjectsListGrid iprojectsGrid, ProjectInfoPane iprojectInfoPane) {
        Window.enableScrolling(false);

        toolbar = itoolbar;
        projectsGrid = iprojectsGrid;
        projectInfoPane = iprojectInfoPane;

        configureLayout();

        bindUIHandlers();
    }

    private void configureLayout() {
        VLayout topPane = new VLayout();
        topPane.setAutoHeight();
        topPane.addMember(toolbar);

        projectsGrid.setWidth(300);
        projectsGrid.setShowResizeBar(true);

        HLayout centerPane = new HLayout();
        centerPane.setMembersMargin(5);
        centerPane.addMember(projectsGrid);
        centerPane.addMember(projectInfoPane);
        projectInfoPane.setVisible(false);

        mainPane = new VLayout();
        mainPane.setWidth100();
        mainPane.setHeight100();
        mainPane.addMember(topPane);
        mainPane.addMember(centerPane);
    }

    private void bindUIHandlers() {
        projectsGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            @Override
            public void onSelectionChanged(SelectionEvent event) {
                ProjectRecord record = (ProjectRecord) projectsGrid.getSelectedRecord();
                projectInfoPane.setVisible(record != null);

                if (record != null) {
                    getUiHandlers().projectSelected(record);
                }
            }
        });

        projectsGrid.addRecordDoubleClickHandler(new RecordDoubleClickHandler() {
            @Override
            public void onRecordDoubleClick(RecordDoubleClickEvent event) {
                getUiHandlers().openProject((ProjectRecord) event.getRecord());
            }
        });
    }

    @Override
    public void setUiHandlers(ProjectsListPageUIHandlers uiHandlers) {
        super.setUiHandlers(uiHandlers);

        toolbar.setUiHandlers(uiHandlers);
        projectInfoPane.setUiHandlers(uiHandlers);
    }

    @Override
    public void setProjects(ProjectDTO[] projects) {
        projectsGrid.setDataFromDTOs(projects);
    }

    @Override
    public ProjectInfoPane getProjectInfoPane() {
        return projectInfoPane;
    }

    @Override
    public Widget asWidget() {
        return mainPane;
    }


}