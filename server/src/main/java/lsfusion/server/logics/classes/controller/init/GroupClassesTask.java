package lsfusion.server.logics.classes.controller.init;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.controller.init.BLGroupSingleSplitTask;
import lsfusion.server.logics.form.struct.FormEntity;

public abstract class GroupClassesTask extends BLGroupSingleSplitTask<CustomClass> {

    @Override
    protected ImSet<CustomClass> getObjects() {
        return getBL().getCustomClasses();
    }
}