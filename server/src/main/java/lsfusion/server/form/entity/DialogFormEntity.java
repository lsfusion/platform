package lsfusion.server.form.entity;

import lsfusion.interop.PropertyEditType;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.form.instance.FormSessionScope;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.mutables.Version;

public class DialogFormEntity<T extends BusinessLogics<T>> extends BaseClassFormEntity<T> {

    public DialogFormEntity(BaseLogicsModule<T> LM, CustomClass cls) {
        super(LM, cls, null, cls.caption);

        object.groupTo.setGridClassView();

//        LM.addObjectActions(this, object);

        Version version = LM.getVersion();

        if(cls.hasStaticObjects())
            addPropertyDraw(LM.staticCaption, version, object);

        setNFEditType(PropertyEditType.READONLY, version);

        if (!cls.dialogReadOnly)
            LM.addFormActions(this, object, FormSessionScope.OLDSESSION);

        finalizeInit(version);
    }
}
