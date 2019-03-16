package lsfusion.server.logics.form.controller.init;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.controller.init.BLGroupSingleSplitTask;
import lsfusion.server.logics.form.struct.FormEntity;

public abstract class GroupFormsTask extends BLGroupSingleSplitTask<FormEntity> {

    @Override
    protected ImSet<FormEntity> getObjects() {
        return getBL().getAllForms();
    }
}
