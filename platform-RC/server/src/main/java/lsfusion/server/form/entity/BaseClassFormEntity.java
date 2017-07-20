package lsfusion.server.form.entity;

import lsfusion.interop.PropertyEditType;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.mutables.Version;

public abstract class BaseClassFormEntity <T extends BusinessLogics<T>> extends FormEntity<T> {

    public final ObjectEntity object;

    protected BaseClassFormEntity(BaseLogicsModule<T> LM, CustomClass cls, String canonicalName, LocalizedString caption) {
        super(canonicalName, caption, LM.getVersion());
        
        Version version = LM.getVersion();

        object = addSingleGroupObject(cls, version, LM.baseGroup, true);

        PropertyDrawEntity objectValue = getNFPropertyDraw(LM.getObjValueProp(this, object), version, object);
        if (objectValue != null)
            objectValue.setEditType(PropertyEditType.READONLY);
    }

}
