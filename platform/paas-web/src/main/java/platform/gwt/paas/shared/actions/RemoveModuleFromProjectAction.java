package platform.gwt.paas.shared.actions;

import net.customware.gwt.dispatch.shared.Action;

public class RemoveModuleFromProjectAction implements Action<GetModulesResult> {
    public int projectId;
    public int moduleId;

    public RemoveModuleFromProjectAction() {
    }

    public RemoveModuleFromProjectAction(int projectId, int moduleId) {
        this.projectId = projectId;
        this.moduleId = moduleId;
    }
}
