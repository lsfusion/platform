package platform.gwt.paas.shared.actions;

import com.gwtplatform.dispatch.shared.UnsecuredActionImpl;

public class RemoveConfigurationAction extends UnsecuredActionImpl<GetConfigurationsResult> {
    public int projectId;
    public int configurationId;

    public RemoveConfigurationAction() {
    }

    public RemoveConfigurationAction(int projectId, int configurationId) {
        this.projectId = projectId;
        this.configurationId = configurationId;
    }
}
