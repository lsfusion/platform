package lsfusion.server.form.entity;

import lsfusion.interop.ClassViewType;
import lsfusion.interop.PropertyEditType;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.form.view.DefaultFormView;
import lsfusion.server.form.view.FormView;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.mutables.Version;

public class EditFormEntity<T extends BusinessLogics<T>> extends BaseClassFormEntity<T> {

    protected EditFormEntity(BaseLogicsModule<T> LM, CustomClass cls, String sID, String caption) {
        super(LM, cls, sID, caption);

        object.groupTo.setSingleClassView(ClassViewType.PANEL);

        PropertyDrawEntity objectValue = getNFPropertyDraw(LM.objectValue, object, LM.getVersion());
        if (objectValue != null)
            objectValue.setEditType(PropertyEditType.READONLY);
    }

    public EditFormEntity(BaseLogicsModule<T> LM, CustomClass cls) {
        this(LM, cls, "editForm_" + cls.getSID(), cls.caption);
    }

    @Override
    public FormView createDefaultRichDesign(Version version) {
        DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign(version);

        design.getDropButton().removeFromParent(version);

        return design;
    }

}
