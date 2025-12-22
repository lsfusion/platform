package lsfusion.server.logics.form.interactive.dialogedit;

import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.classes.user.CustomClass;

public class EditFormEntity extends BaseClassFormEntity {

    public EditFormEntity(BaseLogicsModule LM, CustomClass cls) {
        super(LM, cls, cls.caption);

        object.groupTo.setViewTypePanel(this, baseVersion);

        finalizeInit();

        removeComponent(dropActionPropertyDraw);
    }
}
