package platform.gwt.paas.shared.actions;

import com.gwtplatform.dispatch.shared.UnsecuredActionImpl;

public class StopConfigurationAction extends UnsecuredActionImpl<GetConfigurationsResult> {
    public int configurationId;

    public StopConfigurationAction() {
    }

    public StopConfigurationAction(int configurationId) {
        this.configurationId = configurationId;
    }
}
