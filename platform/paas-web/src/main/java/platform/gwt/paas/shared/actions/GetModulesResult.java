package platform.gwt.paas.shared.actions;

import net.customware.gwt.dispatch.shared.Result;
import paas.api.gwt.shared.dto.ModuleDTO;

public class GetModulesResult implements Result {
    public ModuleDTO[] modules;

    public GetModulesResult() {
    }

    public GetModulesResult(ModuleDTO[] modules) {
        this.modules = modules;
    }
}
