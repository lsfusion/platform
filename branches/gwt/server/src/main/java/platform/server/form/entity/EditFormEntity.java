package platform.server.form.entity;

import platform.interop.ClassViewType;
import platform.server.classes.CustomClass;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.BusinessLogics;

public class EditFormEntity<T extends BusinessLogics<T>> extends BaseClassFormEntity<T> {

    protected final ObjectEntity object;
    protected final String clsSID;

    protected EditFormEntity(BaseLogicsModule<T> LM, CustomClass cls, String sID, String caption) {
        super(LM, cls, sID, caption);

        object = addSingleGroupObject(cls, LM.baseGroup, true);
        object.groupTo.setSingleClassView(ClassViewType.PANEL);

        PropertyDrawEntity objectValue = getPropertyDraw(LM.objectValue, object);
        if (objectValue != null)
            objectValue.readOnly = true;

        clsSID = cls.getSID();
    }

    @Override
    public FormView createDefaultRichDesign() {
        DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

        design.getNullFunction().setVisible(false);

        return design;
    }

    public EditFormEntity(BaseLogicsModule<T> LM, CustomClass cls) {
        this(LM, cls, "objectForm" + cls.getSID(), cls.caption);
    }

    public ObjectEntity getObject() {
        return object;
    }

    @Override
    public AbstractClassFormEntity copy() {
        return new EditFormEntity(LM, cls, getSID() + "_copy" + copies++, caption);
    }
}
