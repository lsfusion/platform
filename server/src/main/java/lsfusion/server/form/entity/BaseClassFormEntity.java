package lsfusion.server.form.entity;

import lsfusion.interop.PropertyEditType;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.mutables.Version;

public abstract class BaseClassFormEntity <T extends BusinessLogics<T>> extends FormEntity<T> {

    public final ObjectEntity object;

    protected BaseClassFormEntity(BaseLogicsModule<T> LM, CustomClass cls, String sID, String caption) {
        super(sID, caption, LM.getVersion());
        
        Version version = LM.getVersion();

        object = addSingleGroupObject(cls, version, LM.baseGroup, true);

        PropertyDrawEntity objectValue = getNFPropertyDraw(LM.objectValue, object, version);
        if (objectValue != null)
            objectValue.setEditType(PropertyEditType.READONLY);
    }

}
