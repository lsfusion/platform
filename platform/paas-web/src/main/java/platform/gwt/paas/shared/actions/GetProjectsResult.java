package platform.gwt.paas.shared.actions;

import net.customware.gwt.dispatch.shared.Result;
import paas.api.gwt.shared.dto.ProjectDTO;

public class GetProjectsResult implements Result {
    public ProjectDTO[] projects;

    public GetProjectsResult() {
    }

    public GetProjectsResult(ProjectDTO[] projects) {
        this.projects = projects;
    }
}
