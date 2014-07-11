package lsfusion.server.logics.tasks;

import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MCol;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.property.Property;

public abstract class GroupFormsTask extends GroupSplitTask<FormEntity> {

    @Override
    protected ImSet<FormEntity> getObjects(BusinessLogics<?> BL) {
        return BL.getFormEntities();
    }
}
