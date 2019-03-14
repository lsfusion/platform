package lsfusion.server.logics.tasks;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.property.Property;

public abstract class GroupPropertiesTask extends BLGroupSingleSplitTask<Property> {

    protected ImSet<Property> getObjects() {
        return getBL().getOrderProperties().getSet();
    }

}
