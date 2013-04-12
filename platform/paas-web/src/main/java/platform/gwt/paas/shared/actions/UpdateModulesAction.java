package platform.gwt.paas.shared.actions;

import net.customware.gwt.dispatch.shared.Action;
import platform.gwt.base.shared.actions.VoidResult;

public class UpdateModulesAction implements Action<VoidResult> {
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
