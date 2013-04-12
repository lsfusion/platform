package platform.gwt.paas.shared.actions;

import net.customware.gwt.dispatch.shared.Action;

public class RemoveProjectAction implements Action<GetProjectsResult> {
    public int projectId;

    public RemoveProjectAction() {
    }

    public RemoveProjectAction(int projectId) {
        this.projectId = projectId;
    }
}
