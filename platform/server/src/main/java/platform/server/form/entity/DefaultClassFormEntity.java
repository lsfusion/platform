package platform.server.form.entity;

import platform.server.classes.CustomClass;
import platform.server.logics.BusinessLogics;

public class DefaultClassFormEntity<T extends BusinessLogics<T>> extends AbstractClassFormEntity<T> {

    protected final ObjectEntity object;
    protected final String clsSID;

    protected DefaultClassFormEntity(T BL, CustomClass cls, int ID, String caption) {
        super(BL, cls, ID, caption);

        object = addSingleGroupObject(cls, BL.baseGroup, true);
        BL.addObjectActions(this, object);

        clsSID = cls.getSID();
    }

    public DefaultClassFormEntity(T BL, CustomClass cls) {
        this(BL, cls, cls.ID + 43132, cls.caption);
    }

    @Override
    public String getSID() {
        return "classForm" + clsSID;
    }

    public ObjectEntity getObject() {
        return object;
    }

    @Override
    public AbstractClassFormEntity copy() {
        return new DefaultClassFormEntity(BL, cls);
    }
}
