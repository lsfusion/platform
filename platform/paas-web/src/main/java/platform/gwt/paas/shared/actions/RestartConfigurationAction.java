package platform.gwt.paas.shared.actions;

import net.customware.gwt.dispatch.shared.Action;
import paas.api.gwt.shared.dto.ConfigurationDTO;

public class RestartConfigurationAction implements Action<GetConfigurationsResult> {
    public ConfigurationDTO configuration;

    public RestartConfigurationAction() {
    }

    public RestartConfigurationAction(ConfigurationDTO configuration) {
        this.configuration = configuration;
    }
}
