package platform.gwt.paas.shared.actions;

import com.gwtplatform.dispatch.shared.UnsecuredActionImpl;

public class GetModuleTextAction extends UnsecuredActionImpl<GetModuleTextResult> {
    public int moduleId;

    public GetModuleTextAction() {
    }

    public GetModuleTextAction(int moduleId) {
        this.moduleId = moduleId;
    }
}
