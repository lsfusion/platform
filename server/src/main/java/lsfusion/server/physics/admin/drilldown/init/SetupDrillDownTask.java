package lsfusion.server.physics.admin.drilldown.init;

import lsfusion.server.physics.admin.SystemProperties;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.init.GroupPropertiesTask;
import lsfusion.server.logics.property.oraction.ActionOrProperty;

public class SetupDrillDownTask extends GroupPropertiesTask {

    public String getCaption() {
        return "Setup drill-down";
    }

    protected void runTask(ActionOrProperty property) {
        if(property instanceof Property)
            getBL().LM.setupDrillDownProperty((Property)property, SystemProperties.lightStart);
    }
}
