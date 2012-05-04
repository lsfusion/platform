package platform.gwt.paas.shared.actions;

import com.gwtplatform.dispatch.shared.UnsecuredActionImpl;

public class UpdateModulesAction extends UnsecuredActionImpl<VoidResult> {
    public int moduleIds[];
    public String moduleTexts[];

    public UpdateModulesAction() {
    }

    public UpdateModulesAction(int moduleId, String moduleText) {
        this.moduleIds = new int[]{moduleId};
        this.moduleTexts = new String[]{moduleText};
    }

    public UpdateModulesAction(int moduleIds[], String moduleTexts[]) {
        this.moduleIds = moduleIds;
        this.moduleTexts = moduleTexts;
    }
}
