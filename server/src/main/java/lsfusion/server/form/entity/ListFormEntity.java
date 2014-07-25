package lsfusion.server.form.entity;

import lsfusion.server.classes.CustomClass;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;

public class ListFormEntity<T extends BusinessLogics<T>> extends BaseClassFormEntity<T> {

    public ListFormEntity(BaseLogicsModule<T> LM, CustomClass cls) {
        super(LM, cls, null, cls.caption);

        LM.addObjectActions(this, object);

        finalizeInit(LM.getVersion());
    }
}
