package platform.gwt.paas.shared.actions;

import com.gwtplatform.dispatch.shared.UnsecuredActionImpl;
import paas.api.gwt.shared.dto.ConfigurationDTO;

public class StartConfigurationAction extends UnsecuredActionImpl<GetConfigurationsResult> {
    public ConfigurationDTO configuration;

    public StartConfigurationAction() {
    }

    public StartConfigurationAction(ConfigurationDTO configuration) {
        this.configuration = configuration;
    }
}
