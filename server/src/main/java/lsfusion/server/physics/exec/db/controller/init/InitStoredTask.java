package lsfusion.server.physics.exec.db.controller.init;

import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.controller.init.GroupPropertiesTask;
import lsfusion.server.logics.property.oraction.ActionOrProperty;

public class InitStoredTask extends GroupPropertiesTask {

    public String getCaption() {
        return "Initializing fields for data and materialized properties";
    }

    protected void runTask(ActionOrProperty property) {
        if (property instanceof Property && ((Property) property).isMarkedStored() && ((Property) property).field == null) { // last check we need because we already initialized some stored (see other initStored usages)
            ((Property) property).initStored(getTableFactory(), getDBNamingPolicy());
        }
    }
}
