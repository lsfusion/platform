package platform.server.form.entity;

import platform.server.classes.CustomClass;
import platform.server.logics.BusinessLogics;

public class ClassFormEntity<T extends BusinessLogics<T>> extends FormEntity<T> {

    public final ObjectEntity object;

    protected ClassFormEntity(T BL, CustomClass cls, int ID, String caption) {
        super(ID, caption);

        GroupObjectEntity groupObject = new GroupObjectEntity(IDShift(1));
        object = new ObjectEntity(IDShift(1),cls,cls.caption);
        groupObject.add(object);
        addGroup(groupObject);

        addPropertyDraw(BL.properties, BL.baseGroup, true, object);
        addPropertyDraw(BL.properties, BL.aggrGroup, true, object);

        BL.addObjectActions(this, object);
    }

    public ClassFormEntity(T BL, CustomClass cls) {
        this(BL, cls, cls.ID + 43132, cls.caption);
    }
}
