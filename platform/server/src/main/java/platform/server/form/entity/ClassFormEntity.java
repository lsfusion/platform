package platform.server.form.entity;

import platform.server.classes.CustomClass;
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.BusinessLogics;

public class ClassFormEntity<T extends BusinessLogics<T>> extends BaseClassFormEntity<T> {

    protected final ObjectEntity object;
    protected final String clsSID;

    protected ClassFormEntity(BaseLogicsModule<T> LM, CustomClass cls, String sID, String caption) {
        super(LM, cls, sID, caption);

        object = addSingleGroupObject(cls, LM.baseGroup, true);
        LM.addObjectActions(this, object, true);

        clsSID = cls.getSID();
    }

    public ClassFormEntity(BaseLogicsModule<T> LM, CustomClass cls) {
        this(LM, cls, "classForm" + cls.getSID(), cls.caption);
    }

    public ObjectEntity getObject() {
        return object;
    }

}
