package platform.gwt.paas.shared.actions;

import com.gwtplatform.dispatch.shared.UnsecuredActionImpl;

public class RemoveModuleFromProjectAction extends UnsecuredActionImpl<GetModulesResult> {
    public int projectId;
    public int moduleId;

    public RemoveModuleFromProjectAction() {
    }

    public RemoveModuleFromProjectAction(int projectId, int moduleId) {
        this.projectId = projectId;
        this.moduleId = moduleId;
    }
}
