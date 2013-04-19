package platform.gwt.paas.client.pages.project.config;

import paas.api.gwt.shared.dto.ConfigurationDTO;

public interface ConfigurationUIHandlers {
    void configurationsUpdated(ConfigurationDTO[] configurations);
}
