package lsfusion.server.logics.tasks;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.logics.BusinessLogics;

public abstract class GroupFormsTask extends GroupSingleSplitTask<FormEntity> {

    @Override
    protected ImSet<FormEntity> getObjects(BusinessLogics BL) {
        return BL.getAllForms();
    }
}
