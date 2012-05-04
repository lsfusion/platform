package platform.gwt.paas.shared.actions;

import com.gwtplatform.dispatch.shared.UnsecuredActionImpl;

public class GetModulesAction extends UnsecuredActionImpl<GetModulesResult> {
    public int projectId;

    public GetModulesAction() {
    }

    public GetModulesAction(int projectId) {
        this.projectId = projectId;
    }
}
