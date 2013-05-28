package platform.gwt.paas.shared.actions;

import net.customware.gwt.dispatch.shared.Action;
import paas.api.gwt.shared.dto.ProjectDTO;

public class UpdateProjectAction implements Action<GetProjectsResult> {
    public ProjectDTO project;

    public UpdateProjectAction() {
    }

    public UpdateProjectAction(ProjectDTO project) {
        this.project = project;
    }
}
