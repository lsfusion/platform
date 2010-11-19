package platform.server.form.entity;

import platform.server.classes.CustomClass;
import platform.server.logics.BusinessLogics;

public class ClassFormEntity<T extends BusinessLogics<T>> extends FormEntity<T> {

    public final ObjectEntity object;

    protected ClassFormEntity(T BL, CustomClass cls, int ID, String caption) {
        super(ID, caption);

        object = addSingleGroupObject(cls, BL.baseGroup, true);
        BL.addObjectActions(this, object);
    }

    public ClassFormEntity(T BL, CustomClass cls) {
        this(BL, cls, cls.ID + 43132, cls.caption);
    }
}
