package platform.gwt.paas.shared.actions;

import net.customware.gwt.dispatch.shared.Action;

public class GetAvailableModulesAction implements Action<GetModulesResult> {
    public int projectId;

    public GetAvailableModulesAction() {
    }

    public GetAvailableModulesAction(int projectId) {
        this.projectId = projectId;
    }
}
