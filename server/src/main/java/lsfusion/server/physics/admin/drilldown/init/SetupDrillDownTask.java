package lsfusion.server.physics.admin.drilldown.init;

import lsfusion.server.SystemProperties;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.oraction.Property;
import lsfusion.server.logics.property.init.GroupPropertiesTask;

public class SetupDrillDownTask extends GroupPropertiesTask {

    public String getCaption() {
        return "Setup drill-down";
    }

    protected void runTask(Property property) {
        if(property instanceof CalcProperty)
            getBL().LM.setupDrillDownProperty((CalcProperty)property, SystemProperties.lightStart);
    }
}
