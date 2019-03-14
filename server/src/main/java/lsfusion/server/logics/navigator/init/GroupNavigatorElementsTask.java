package lsfusion.server.logics.navigator.init;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.init.BLGroupSingleSplitTask;
import lsfusion.server.logics.navigator.NavigatorElement;

public abstract class GroupNavigatorElementsTask extends BLGroupSingleSplitTask<NavigatorElement> {

    @Override
    protected ImSet<NavigatorElement> getObjects() {
        return getBL().getNavigatorElements();
    }
}
