package platform.server.form.entity;

import platform.interop.ClassViewType;
import platform.server.classes.CustomClass;
import platform.server.logics.BusinessLogics;

public class DialogFormEntity<T extends BusinessLogics<T>> extends AbstractClassFormEntity<T> {

    protected final ObjectEntity object;
    protected final String clsSID;

    protected DialogFormEntity(T BL, CustomClass cls, int ID, String caption) {
        super(BL, cls, ID, caption);

        object = addSingleGroupObject(cls, BL.baseGroup, true);
        object.groupTo.setSingleClassView(ClassViewType.GRID);

        BL.addObjectActions(this, object);

        PropertyDrawEntity objectValue = getPropertyDraw(BL.objectValue, object);
        if (objectValue != null)
            objectValue.readOnly = true;

        clsSID = cls.getSID();
    }

    public DialogFormEntity(T BL, CustomClass cls) {
        this(BL, cls, cls.ID + 53132, cls.caption);
    }

    @Override
    public String getSID() {
        return "dialogForm" + clsSID;
    }

    public ObjectEntity getObject() {
        return object;
    }

    @Override
    public AbstractClassFormEntity copy() {
        return new DialogFormEntity(BL, cls);
    }
}
