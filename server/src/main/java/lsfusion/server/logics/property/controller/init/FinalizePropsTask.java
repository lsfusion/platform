package lsfusion.server.logics.property.controller.init;

import lsfusion.server.logics.property.oraction.ActionOrProperty;

public class FinalizePropsTask extends GroupPropertiesTask {
    public String getCaption() {
        return "Finalizing properties";
    }

    protected void runTask(ActionOrProperty property) {
        property.finalizeAroundInit();
    }
}
