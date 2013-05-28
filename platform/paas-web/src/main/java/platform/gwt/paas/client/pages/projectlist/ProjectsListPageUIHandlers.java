package platform.gwt.paas.client.pages.projectlist;

import com.gwtplatform.mvp.client.UiHandlers;
import platform.gwt.paas.client.data.ProjectRecord;

public interface ProjectsListPageUIHandlers extends UiHandlers {
    void projectSelected(ProjectRecord record);

    void addNewProject();

    void refreshButtonClicked();

    void openProject(ProjectRecord selectedProject);

    void deleteProject(ProjectRecord selectedProject);

    void editProject(ProjectRecord selectedProject);
}
