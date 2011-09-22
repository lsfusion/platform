package platform.gwt.paas.shared.actions;

import com.gwtplatform.dispatch.shared.UnsecuredActionImpl;
import paas.api.gwt.shared.dto.ConfigurationDTO;

public class UpdateConfigurationAction extends UnsecuredActionImpl<GetConfigurationsResult> {
    public ConfigurationDTO configuration;

    public UpdateConfigurationAction() {
    }

    public UpdateConfigurationAction(ConfigurationDTO configuration) {
        this.configuration = configuration;
    }
}
