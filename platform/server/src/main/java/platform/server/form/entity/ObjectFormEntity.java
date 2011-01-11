package platform.server.form.entity;

import platform.interop.ClassViewType;
import platform.server.classes.CustomClass;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.logics.BusinessLogics;

public class ObjectFormEntity<T extends BusinessLogics<T>> extends AbstractClassFormEntity<T> {

    protected final ObjectEntity object;
    protected final String clsSID;

    protected ObjectFormEntity(T BL, CustomClass cls, int ID, String caption) {
        super(BL, cls, ID, caption);

        object = addSingleGroupObject(cls, BL.baseGroup, true);
        object.groupTo.setSingleClassView(ClassViewType.PANEL);

        getPropertyDraw(BL.objectValue, object).readOnly = true;

        clsSID = cls.getSID();
    }

    @Override
    public FormView createDefaultRichDesign() {
        DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

        design.getNullFunction().setVisible(false);

        return design;
    }

    public ObjectFormEntity(T BL, CustomClass cls) {
        this(BL, cls, cls.ID + 63132, cls.caption);
    }

    @Override
    public String getSID() {
        return "objectForm" + clsSID;
    }

    public ObjectEntity getObject() {
        return object;
    }

    @Override
    public AbstractClassFormEntity copy() {
        return new ObjectFormEntity(BL, cls);
    }
}
