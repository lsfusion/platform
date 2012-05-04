package platform.gwt.paas.shared.actions;

import com.gwtplatform.dispatch.shared.UnsecuredActionImpl;

public class RemoveProjectAction extends UnsecuredActionImpl<GetProjectsResult> {
    public int projectId;

    public RemoveProjectAction() {
    }

    public RemoveProjectAction(int projectId) {
        this.projectId = projectId;
    }
}
