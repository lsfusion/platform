package lsfusion.server.logics.property.controller.init;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.controller.init.BLGroupSingleSplitTask;
import lsfusion.server.logics.property.oraction.ActionOrProperty;

public abstract class GroupPropertiesTask extends BLGroupSingleSplitTask<ActionOrProperty> {

    protected ImSet<ActionOrProperty> getObjects() {
        return getBL().getOrderActionOrProperties().getSet();
    }

    @Override
    protected int getSplitCount() {
        return 1000;
    }
}
