package platform.server.form.entity;

import platform.interop.PropertyEditType;
import platform.server.classes.CustomClass;
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.BusinessLogics;

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
