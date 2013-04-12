package platform.gwt.paas.shared.actions;

import net.customware.gwt.dispatch.shared.Action;

public class AddNewModuleAction implements Action<GetModulesResult> {
    public int projectId;
    public String moduleName;

    public AddNewModuleAction() {
    }

    public AddNewModuleAction(int projectId, String moduleName) {
        this.projectId = projectId;
        this.moduleName = moduleName;
    }
}
