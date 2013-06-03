package lsfusion.server.form.entity;

import lsfusion.interop.PropertyEditType;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;

public abstract class BaseClassFormEntity <T extends BusinessLogics<T>> extends FormEntity<T> {

    public final ObjectEntity object;

    protected BaseClassFormEntity(BaseLogicsModule<T> LM, CustomClass cls, String sID, String caption) {
        super(sID, caption);

        object = addSingleGroupObject(cls, LM.baseGroup, true);

        PropertyDrawEntity objectValue = getPropertyDraw(LM.objectValue, object);
        if (objectValue != null)
            objectValue.setEditType(PropertyEditType.READONLY);
    }

}
