package platform.gwt.paas.shared.actions;

import net.customware.gwt.dispatch.shared.Action;

public class RemoveConfigurationAction implements Action<GetConfigurationsResult> {
    public int projectId;
    public int configurationId;

    public RemoveConfigurationAction() {
    }

    public RemoveConfigurationAction(int projectId, int configurationId) {
        this.projectId = projectId;
        this.configurationId = configurationId;
    }
}
