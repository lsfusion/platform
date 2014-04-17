package lsfusion.server.form.entity;

import lsfusion.interop.ClassViewType;
import lsfusion.interop.PropertyEditType;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.mutables.Version;

public class DialogFormEntity<T extends BusinessLogics<T>> extends BaseClassFormEntity<T> {

    protected DialogFormEntity(BaseLogicsModule<T> LM, CustomClass cls, String sID, String caption) {
        super(LM, cls, sID, caption);

        object.groupTo.setSingleClassView(ClassViewType.GRID);

//        LM.addObjectActions(this, object);

        Version version = LM.getVersion();

        setNFEditType(PropertyEditType.READONLY, version);

        if (!cls.dialogReadOnly)
            LM.addFormActions(this, object);

        finalizeInit(version);
    }

    public DialogFormEntity(BaseLogicsModule<T> LM, CustomClass cls) {
        this(LM, cls, "dialogForm_" + cls.getSID(), cls.caption);
    }
}
