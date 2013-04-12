package platform.gwt.paas.shared.actions;

import net.customware.gwt.dispatch.shared.Action;
import paas.api.gwt.shared.dto.ConfigurationDTO;

public class UpdateConfigurationAction implements Action<GetConfigurationsResult> {
    public ConfigurationDTO configuration;

    public UpdateConfigurationAction() {
    }

    public UpdateConfigurationAction(ConfigurationDTO configuration) {
        this.configuration = configuration;
    }
}
