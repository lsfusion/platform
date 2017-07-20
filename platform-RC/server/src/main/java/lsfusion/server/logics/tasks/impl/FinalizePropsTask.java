package lsfusion.server.logics.tasks.impl;

import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.tasks.GroupPropertiesTask;

public class FinalizePropsTask extends GroupPropertiesTask {
    public String getCaption() {
        return "Finalizing properties";
    }

    protected void runTask(Property property) {
        property.finalizeAroundInit();
    }
}
