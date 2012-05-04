package platform.gwt.paas.shared.actions;

import com.gwtplatform.dispatch.shared.UnsecuredActionImpl;
import paas.api.gwt.shared.dto.ConfigurationDTO;

public class RestartConfigurationAction extends UnsecuredActionImpl<GetConfigurationsResult> {
    public ConfigurationDTO configuration;

    public RestartConfigurationAction() {
    }

    public RestartConfigurationAction(ConfigurationDTO configuration) {
        this.configuration = configuration;
    }
}
