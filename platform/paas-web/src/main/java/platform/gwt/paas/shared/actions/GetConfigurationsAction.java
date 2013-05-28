package platform.gwt.paas.shared.actions;

import net.customware.gwt.dispatch.shared.Action;

public class GetConfigurationsAction implements Action<GetConfigurationsResult> {
    public int projectId;

    public GetConfigurationsAction() {
    }

    public GetConfigurationsAction(int projectId) {
        this.projectId = projectId;
    }
}
