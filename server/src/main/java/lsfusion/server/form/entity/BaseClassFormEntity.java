package lsfusion.server.form.entity;

import lsfusion.interop.PropertyEditType;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.mutables.Version;

public abstract class BaseClassFormEntity <T extends BusinessLogics<T>> extends FormEntity {

    public final ObjectEntity object;

    protected BaseClassFormEntity(BaseLogicsModule<T> LM, CustomClass cls, String canonicalName, LocalizedString caption) {
        super(canonicalName, caption, LM.getVersion());
        
        Version version = LM.getVersion();

        object = addSingleGroupObject(cls, version);

        // нужно, чтобы всегда была хоть одно свойство (иначе если нет ни одного base grid'ы не показываются)
        LCP objValueProp = LM.getObjValueProp(this, object);
        PropertyDrawEntitrdffdy objectValue = addPropertyDraw(objValueProp, version, object);
        objectValue.setEditType(PropertyEditType.READONLY);
        
        addPropertyDraw(object, version, LM.baseGroup, true);
    }

}
