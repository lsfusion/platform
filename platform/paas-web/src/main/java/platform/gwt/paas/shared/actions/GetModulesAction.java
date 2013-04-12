package platform.gwt.paas.shared.actions;

import net.customware.gwt.dispatch.shared.Action;

public class GetModulesAction implements Action<GetModulesResult> {
    public int projectId;

    public GetModulesAction() {
    }

    public GetModulesAction(int projectId) {
        this.projectId = projectId;
    }
}
