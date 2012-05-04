package platform.gwt.paas.shared.actions;

import com.gwtplatform.dispatch.shared.UnsecuredActionImpl;

public class AddModulesAction extends UnsecuredActionImpl<GetModulesResult> {
    public int projectId;
    public int[] moduleIds;

    public AddModulesAction() {
    }

    public AddModulesAction(int projectId, int... moduleIds) {
        this.projectId = projectId;
        this.moduleIds = moduleIds;
    }
}
