package platform.server.form.entity;

import platform.server.classes.CustomClass;
import platform.server.logics.BusinessLogics;

public class ClassFormEntity<T extends BusinessLogics<T>> extends FormEntity<T> {

    public final ObjectEntity object;
    private final String clsSID;

    protected ClassFormEntity(T BL, CustomClass cls, int ID, String caption) {
        super(ID, caption);

        object = addSingleGroupObject(cls, BL.baseGroup, true);
        BL.addObjectActions(this, object);

        clsSID = cls.getSID();
    }

    public ClassFormEntity(T BL, CustomClass cls) {
        this(BL, cls, cls.ID + 43132, cls.caption);
    }

    @Override
    public String getSID() {
        return "classForm" + clsSID;
    }
}
