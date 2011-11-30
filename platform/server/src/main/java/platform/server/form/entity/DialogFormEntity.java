package platform.server.form.entity;

import platform.interop.ClassViewType;
import platform.server.classes.CustomClass;
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.BusinessLogics;

public class DialogFormEntity<T extends BusinessLogics<T>> extends BaseClassFormEntity<T> {

    protected DialogFormEntity(BaseLogicsModule<T> LM, CustomClass cls, String sID, String caption) {
        super(LM, cls, sID, caption);

        object.groupTo.setSingleClassView(ClassViewType.GRID);

        LM.addObjectActions(this, object);

//        setReadOnly(true);
//
//        LM.addFormActions(this, object);
    }

    public DialogFormEntity(BaseLogicsModule<T> LM, CustomClass cls) {
        this(LM, cls, "dialogForm_" + cls.getSID(), cls.caption);
    }

    public ObjectEntity getObject() {
        return object;
    }

}
