package platform.server.form.entity;

import platform.server.classes.CustomClass;
import platform.server.logics.BusinessLogics;

public class ClassFormEntity<T extends BusinessLogics<T>> extends FormEntity<T> {

    public final ObjectEntity object;

    protected ClassFormEntity(T BL, CustomClass cls, int ID, String caption) {
        super(ID, caption);

        GroupObjectEntity groupObject = new GroupObjectEntity(genID());
        object = new ObjectEntity(genID(),cls,cls.caption);
        groupObject.add(object);
        addGroup(groupObject);

        addPropertyDraw(BL.baseGroup, true, object);

        BL.addObjectActions(this, object);
    }

    public ClassFormEntity(T BL, CustomClass cls) {
        this(BL, cls, cls.ID + 43132, cls.caption);
    }
}
