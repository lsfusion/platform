package lsfusion.server.physics.admin.drilldown.controller.init;

import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.controller.init.GroupPropertiesTask;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.physics.admin.SystemProperties;

public class SetupDrillDownTask extends GroupPropertiesTask {

    public String getCaption() {
        return "Setup drill-down";
    }

    protected void runTask(ActionOrProperty property) {
        if(property instanceof Property)
            getBL().LM.setupDrillDownProperty((Property)property, SystemProperties.lightStart);
    }
}
