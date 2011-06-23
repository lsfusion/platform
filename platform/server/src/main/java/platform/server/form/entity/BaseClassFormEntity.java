package platform.server.form.entity;

import platform.server.classes.CustomClass;
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.BusinessLogics;

public abstract class BaseClassFormEntity <T extends BusinessLogics<T>> extends ClassFormEntity<T> {

    protected final ObjectEntity object;

    protected BaseClassFormEntity(BaseLogicsModule<T> LM, CustomClass cls, String sID, String caption) {
        super(sID, caption);

        object = addSingleGroupObject(cls, LM.baseGroup, true);

        PropertyDrawEntity objectValue = getPropertyDraw(LM.objectValue, object);
        if (objectValue != null)
            objectValue.readOnly = true;
    }

}
