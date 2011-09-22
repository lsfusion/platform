package platform.gwt.paas.shared.actions;

import com.gwtplatform.dispatch.shared.UnsecuredActionImpl;

public class GetConfigurationsAction extends UnsecuredActionImpl<GetConfigurationsResult> {
    public int projectId;

    public GetConfigurationsAction() {
    }

    public GetConfigurationsAction(int projectId) {
        this.projectId = projectId;
    }
}
