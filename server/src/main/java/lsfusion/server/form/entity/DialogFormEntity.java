package lsfusion.server.form.entity;

import lsfusion.interop.ClassViewType;
import lsfusion.interop.PropertyEditType;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;

public class DialogFormEntity<T extends BusinessLogics<T>> extends BaseClassFormEntity<T> {

    protected DialogFormEntity(BaseLogicsModule<T> LM, CustomClass cls, String sID, String caption) {
        super(LM, cls, sID, caption);

        object.groupTo.setSingleClassView(ClassViewType.GRID);

//        LM.addObjectActions(this, object);

        setEditType(PropertyEditType.READONLY);

        if (!cls.dialogReadOnly)
            LM.addFormActions(this, object);
    }

    public DialogFormEntity(BaseLogicsModule<T> LM, CustomClass cls) {
        this(LM, cls, "dialogForm_" + cls.getSID(), cls.caption);
    }
}
