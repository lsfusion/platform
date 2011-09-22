package platform.gwt.paas.shared.actions;

import com.gwtplatform.dispatch.shared.UnsecuredActionImpl;
import paas.api.gwt.shared.dto.ProjectDTO;

public class UpdateProjectAction extends UnsecuredActionImpl<GetProjectsResult> {
    public ProjectDTO project;

    public UpdateProjectAction() {
    }

    public UpdateProjectAction(ProjectDTO project) {
        this.project = project;
    }
}
