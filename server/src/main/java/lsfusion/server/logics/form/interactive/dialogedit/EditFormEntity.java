package lsfusion.server.logics.form.interactive.dialogedit;

import lsfusion.server.base.version.Version;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.interactive.design.auto.DefaultFormView;

public class EditFormEntity extends BaseClassFormEntity {

    public EditFormEntity(BaseLogicsModule LM, CustomClass cls) {
        super(LM, cls, cls.caption);

        object.groupTo.setViewTypePanel();

        finalizeInit();

        removeComponent(dropActionPropertyDraw);
    }
}
