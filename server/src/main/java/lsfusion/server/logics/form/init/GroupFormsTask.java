package lsfusion.server.logics.form.init;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.controller.init.BLGroupSingleSplitTask;

public abstract class GroupFormsTask extends BLGroupSingleSplitTask<FormEntity> {

    @Override
    protected ImSet<FormEntity> getObjects() {
        return getBL().getAllForms();
    }
}
