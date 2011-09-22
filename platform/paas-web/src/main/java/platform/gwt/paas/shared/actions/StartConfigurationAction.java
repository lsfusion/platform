package platform.gwt.paas.shared.actions;

import com.gwtplatform.dispatch.shared.UnsecuredActionImpl;

public class StartConfigurationAction extends UnsecuredActionImpl<GetConfigurationsResult> {
    public int configurationId;

    public StartConfigurationAction() {
    }

    public StartConfigurationAction(int configurationId) {
        this.configurationId = configurationId;
    }
}
