package platform.gwt.paas.shared.actions;

import net.customware.gwt.dispatch.shared.Action;

public class GetModuleTextAction implements Action<GetModuleTextResult> {
    public int moduleId;

    public GetModuleTextAction() {
    }

    public GetModuleTextAction(int moduleId) {
        this.moduleId = moduleId;
    }
}
