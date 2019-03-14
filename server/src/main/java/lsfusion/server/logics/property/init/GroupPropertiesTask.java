package lsfusion.server.logics.property.init;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.init.BLGroupSingleSplitTask;
import lsfusion.server.logics.property.oraction.ActionOrProperty;

public abstract class GroupPropertiesTask extends BLGroupSingleSplitTask<ActionOrProperty> {

    protected ImSet<ActionOrProperty> getObjects() {
        return getBL().getOrderProperties().getSet();
    }

}
