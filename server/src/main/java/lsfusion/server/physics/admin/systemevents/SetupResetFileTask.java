package lsfusion.server.physics.admin.systemevents;

import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.controller.init.GroupPropertiesTask;
import lsfusion.server.logics.property.oraction.ActionOrProperty;

public class SetupResetFileTask extends GroupPropertiesTask {

    public String getCaption() {
        return "Setup reset files";
    }

    protected void runTask(ActionOrProperty property) {
        if(property instanceof Property)
            getBL().LM.setupResetProperty((Property)property);
    }
}