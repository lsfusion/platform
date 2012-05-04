package platform.gwt.paas.shared.actions;

import com.gwtplatform.dispatch.shared.UnsecuredActionImpl;

public class GetAvailableModulesAction extends UnsecuredActionImpl<GetModulesResult> {
    public int projectId;

    public GetAvailableModulesAction() {
    }

    public GetAvailableModulesAction(int projectId) {
        this.projectId = projectId;
    }
}
