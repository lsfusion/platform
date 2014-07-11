package lsfusion.server.logics.tasks;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.property.Property;

public abstract class GroupPropertiesTask extends GroupSplitTask<Property> {

    protected ImSet<Property> getObjects(BusinessLogics<?> BL) {
        return BL.getOrderProperties().getSet();
    }

}
