package lsfusion.server.logics.form.auto;

import lsfusion.interop.form.property.PropertyEditType;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.logics.form.interactive.action.edit.FormSessionScope;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.mutables.Version;

public class DialogFormEntity extends BaseClassFormEntity {

    public DialogFormEntity(BaseLogicsModule LM, CustomClass cls) {
        super(LM, cls, null, cls.caption);

        object.groupTo.setGridClassView();

//        LM.addObjectActions(this, object);

        Version version = LM.getVersion();

        setNFEditType(PropertyEditType.READONLY, version);

        if (!cls.dialogReadOnly)
            LM.addFormActions(this, object, FormSessionScope.NEWSESSION);

        finalizeInit(version);
    }
}
