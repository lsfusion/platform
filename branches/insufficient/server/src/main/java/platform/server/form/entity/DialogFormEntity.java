package platform.server.form.entity;

import platform.interop.ClassViewType;
import platform.server.classes.CustomClass;
import platform.server.logics.BusinessLogics;

public class DialogFormEntity<T extends BusinessLogics<T>> extends AbstractClassFormEntity<T> {

    protected final ObjectEntity object;
    protected final String clsSID;

    protected DialogFormEntity(T BL, CustomClass cls, String sID, String caption) {
        super(BL, cls, sID, caption);

        object = addSingleGroupObject(cls, BL.baseGroup, true);
        object.groupTo.setSingleClassView(ClassViewType.GRID);

        BL.addObjectActions(this, object);

        PropertyDrawEntity objectValue = getPropertyDraw(BL.objectValue, object);
        if (objectValue != null)
            objectValue.readOnly = true;

        clsSID = cls.getSID();
    }

    public DialogFormEntity(T BL, CustomClass cls) {
        this(BL, cls, "classForm" + cls.getSID() + "_dialog", cls.caption);
    }

    public ObjectEntity getObject() {
        return object;
    }

    @Override
    public AbstractClassFormEntity copy() {
        return new DialogFormEntity<T>(BL, cls, getSID() + "_copy" + copies++, caption);
    }
}
