package lsfusion.server.logics.form.auto.drilldown.init;

import lsfusion.server.SystemProperties;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.init.GroupPropertiesTask;

public class SetupDrillDownTask extends GroupPropertiesTask {

    public String getCaption() {
        return "Setup drill-down";
    }

    protected void runTask(Property property) {
        getBL().LM.setupDrillDownProperty(property, SystemProperties.lightStart);
    }
}
