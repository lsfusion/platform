package lsfusion.server.logics.tasks;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.form.navigator.NavigatorElement;
import lsfusion.server.logics.BusinessLogics;

public abstract class GroupNavigatorElementsTask extends GroupSingleSplitTask<NavigatorElement> {

    @Override
    protected ImSet<NavigatorElement> getObjects(BusinessLogics<?> BL) {
        return BL.getNavigatorElements();
    }
}
