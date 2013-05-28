package platform.gwt.paas.shared.actions;

import net.customware.gwt.dispatch.shared.Action;

public class AddModulesAction implements Action<GetModulesResult> {
    public int projectId;
    public int[] moduleIds;

    public AddModulesAction() {
    }

    public AddModulesAction(int projectId, int... moduleIds) {
        this.projectId = projectId;
        this.moduleIds = moduleIds;
    }
}
