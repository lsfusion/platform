package platform.gwt.paas.shared.actions;

import net.customware.gwt.dispatch.shared.Action;
import paas.api.gwt.shared.dto.ConfigurationDTO;

public class StartConfigurationAction implements Action<GetConfigurationsResult> {
    public ConfigurationDTO configuration;

    public StartConfigurationAction() {
    }

    public StartConfigurationAction(ConfigurationDTO configuration) {
        this.configuration = configuration;
    }
}
