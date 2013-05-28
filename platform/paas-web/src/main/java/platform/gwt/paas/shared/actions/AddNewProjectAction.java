package platform.gwt.paas.shared.actions;

import net.customware.gwt.dispatch.shared.Action;
import paas.api.gwt.shared.dto.ProjectDTO;

public class AddNewProjectAction implements Action<GetProjectsResult> {
    public ProjectDTO project;

    public AddNewProjectAction() {
    }

    public AddNewProjectAction(ProjectDTO project) {
        this.project = project;
    }
}
