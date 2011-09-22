package platform.gwt.paas.shared.actions;

import com.gwtplatform.dispatch.shared.UnsecuredActionImpl;

public class AddNewModuleAction extends UnsecuredActionImpl<GetModulesResult> {
    public int projectId;
    public String moduleName;

    public AddNewModuleAction() {
    }

    public AddNewModuleAction(int projectId, String moduleName) {
        this.projectId = projectId;
        this.moduleName = moduleName;
    }
}
