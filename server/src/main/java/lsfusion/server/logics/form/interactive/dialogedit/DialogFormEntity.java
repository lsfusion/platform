package lsfusion.server.logics.form.interactive.dialogedit;

import lsfusion.interop.form.property.PropertyEditType;
import lsfusion.server.base.version.Version;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.interactive.action.edit.FormSessionScope;

public class DialogFormEntity extends BaseClassFormEntity {

    public DialogFormEntity(BaseLogicsModule LM, CustomClass cls) {
        super(LM, cls, cls.caption);

        object.groupTo.setViewTypeList();

//        LM.addObjectActions(this, object);

        Version version = LM.getVersion();

        setNFEditType(PropertyEditType.READONLY, version);

        if (!cls.dialogReadOnly)
            LM.addFormActions(this, object, FormSessionScope.NEWSESSION);

        finalizeInit(version);
    }
}
