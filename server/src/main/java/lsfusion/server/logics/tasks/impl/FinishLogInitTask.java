package lsfusion.server.logics.tasks.impl;

import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.tasks.GroupPropertiesTask;

public class FinishLogInitTask extends GroupPropertiesTask {
    public String getCaption() {
        return "Setup loggables";
    }

    protected void runTask(Property property) {
        getBL().finishLogInit(property);
    }
}
