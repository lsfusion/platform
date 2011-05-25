package platform.server.form.entity;

import platform.interop.ClassViewType;
import platform.server.classes.CustomClass;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.logics.BusinessLogics;

public class EditFormEntity<T extends BusinessLogics<T>> extends AbstractClassFormEntity<T> {

    protected final ObjectEntity object;
    protected final String clsSID;

    protected EditFormEntity(T BL, CustomClass cls, String sID, String caption) {
        super(BL, cls, sID, caption);

        object = addSingleGroupObject(cls, BL.LM.baseGroup, true);
        object.groupTo.setSingleClassView(ClassViewType.PANEL);

        PropertyDrawEntity objectValue = getPropertyDraw(BL.LM.objectValue, object);
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

    public EditFormEntity(T BL, CustomClass cls) {
        this(BL, cls, "objectForm" + cls.getSID(), cls.caption);
    }

    public ObjectEntity getObject() {
        return object;
    }

    @Override
    public AbstractClassFormEntity copy() {
        return new EditFormEntity(BL, cls, getSID() + "_copy" + copies++, caption);
    }
}
