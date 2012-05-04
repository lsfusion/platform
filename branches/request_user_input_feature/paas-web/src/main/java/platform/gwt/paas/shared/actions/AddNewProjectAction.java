package platform.gwt.paas.shared.actions;

import com.gwtplatform.dispatch.shared.UnsecuredActionImpl;
import paas.api.gwt.shared.dto.ProjectDTO;

public class AddNewProjectAction extends UnsecuredActionImpl<GetProjectsResult> {
    public ProjectDTO project;

    public AddNewProjectAction() {
    }

    public AddNewProjectAction(ProjectDTO project) {
        this.project = project;
    }
}
