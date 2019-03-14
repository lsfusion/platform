package lsfusion.server.logics.form.interactive.dialogedit;

import lsfusion.server.logics.classes.CustomClass;
import lsfusion.server.logics.form.interactive.design.auto.DefaultFormView;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.base.version.Version;

public class EditFormEntity extends BaseClassFormEntity {

    public EditFormEntity(BaseLogicsModule LM, CustomClass cls) {
        super(LM, cls, null, cls.caption);

        object.groupTo.setPanelClassView();

        finalizeInit(LM.getVersion());
    }

    @Override
    public FormView createDefaultRichDesign(Version version) {
        DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign(version);

        design.getDropButton().removeFromParent(version);

        return design;
    }

}
