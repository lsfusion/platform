package platform.server.form.entity;

import platform.interop.ClassViewType;
import platform.server.classes.CustomClass;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.BusinessLogics;

public class EditFormEntity<T extends BusinessLogics<T>> extends BaseClassFormEntity<T> {

    protected EditFormEntity(BaseLogicsModule<T> LM, CustomClass cls, String sID, String caption) {
        super(LM, cls, sID, caption);

        object.groupTo.setSingleClassView(ClassViewType.PANEL);

        PropertyDrawEntity objectValue = getPropertyDraw(LM.objectValue, object);
        if (objectValue != null)
            objectValue.readOnly = true;
    }

    public EditFormEntity(BaseLogicsModule<T> LM, CustomClass cls) {
        this(LM, cls, "editForm_" + cls.getSID(), cls.caption);
    }

    @Override
    public FormView createDefaultRichDesign() {
        DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

        design.getNullFunction().setVisible(false);

        return design;
    }

    public ObjectEntity getObject() {
        return object;
    }

}
