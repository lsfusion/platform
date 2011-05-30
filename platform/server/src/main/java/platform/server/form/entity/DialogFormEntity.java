package platform.server.form.entity;

import platform.interop.ClassViewType;
import platform.server.classes.CustomClass;
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.BusinessLogics;

public class DialogFormEntity<T extends BusinessLogics<T>> extends BaseClassFormEntity<T> {

    protected final ObjectEntity object;
    protected final String clsSID;

    protected DialogFormEntity(BaseLogicsModule<T> LM, CustomClass cls, String sID, String caption) {
        super(LM, cls, sID, caption);

        object = addSingleGroupObject(cls, LM.baseGroup, true);
        object.groupTo.setSingleClassView(ClassViewType.GRID);

        LM.addObjectActions(this, object);

        PropertyDrawEntity objectValue = getPropertyDraw(LM.objectValue, object);
        if (objectValue != null)
            objectValue.readOnly = true;

        clsSID = cls.getSID();
    }

    public DialogFormEntity(BaseLogicsModule<T> LM, CustomClass cls) {
        this(LM, cls, "classForm" + cls.getSID() + "_dialog", cls.caption);
    }

    public ObjectEntity getObject() {
        return object;
    }

    @Override
    public AbstractClassFormEntity copy() {
        return new DialogFormEntity<T>(LM, cls, getSID() + "_copy" + copies++, caption);
    }
}
