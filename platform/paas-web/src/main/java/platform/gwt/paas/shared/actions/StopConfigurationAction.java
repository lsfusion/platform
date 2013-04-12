package platform.gwt.paas.shared.actions;

import net.customware.gwt.dispatch.shared.Action;

public class StopConfigurationAction implements Action<GetConfigurationsResult> {
    public int configurationId;

    public StopConfigurationAction() {
    }

    public StopConfigurationAction(int configurationId) {
        this.configurationId = configurationId;
    }
}
