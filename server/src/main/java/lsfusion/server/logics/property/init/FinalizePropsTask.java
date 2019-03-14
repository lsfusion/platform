package lsfusion.server.logics.property.init;

import lsfusion.server.logics.property.oraction.Property;

public class FinalizePropsTask extends GroupPropertiesTask {
    public String getCaption() {
        return "Finalizing properties";
    }

    protected void runTask(Property property) {
        property.finalizeAroundInit();
    }
}
