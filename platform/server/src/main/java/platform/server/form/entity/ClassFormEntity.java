package platform.server.form.entity;

import platform.server.classes.CustomClass;
import platform.server.logics.BusinessLogics;

public class ClassFormEntity<T extends BusinessLogics<T>> extends AbstractClassFormEntity<T> {

    protected final ObjectEntity object;
    protected final String clsSID;

    protected ClassFormEntity(T BL, CustomClass cls, String sID, String caption) {
        super(BL, cls, sID, caption);

        object = addSingleGroupObject(cls, BL.baseGroup, true);
        BL.addObjectActions(this, object);

        clsSID = cls.getSID();
    }

    public ClassFormEntity(T BL, CustomClass cls) {
        this(BL, cls, "classForm" + cls.getSID(), cls.caption);
    }

    public ObjectEntity getObject() {
        return object;
    }

    @Override
    public AbstractClassFormEntity copy() {
        return new ClassFormEntity(BL, cls, getSID() + "_copy" + copies++, caption);
    }
}
