package platform.gwt.paas.shared.actions;

import net.customware.gwt.dispatch.shared.Result;
import paas.api.gwt.shared.dto.ConfigurationDTO;

public class GetConfigurationsResult implements Result {
    public ConfigurationDTO[] configurations;

    public GetConfigurationsResult() {
    }

    public GetConfigurationsResult(ConfigurationDTO[] configurations) {
        this.configurations = configurations;
    }
}
