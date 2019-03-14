package lsfusion.server.physics.admin.logging.init;

import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.oraction.Property;
import lsfusion.server.logics.property.init.GroupPropertiesTask;

public class FinishLogInitTask extends GroupPropertiesTask {
    public String getCaption() {
        return "Setup loggables";
    }

    protected void runTask(Property property) {
        if(property instanceof CalcProperty)
            getBL().finishLogInit((CalcProperty) property);
    }
}
