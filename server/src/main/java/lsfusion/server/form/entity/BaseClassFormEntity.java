package lsfusion.server.form.entity;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.interop.PropertyEditType;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.PropertyClassImplement;

public abstract class BaseClassFormEntity extends FormEntity {

    public final ObjectEntity object;

    protected BaseClassFormEntity(BaseLogicsModule LM, CustomClass cls, String canonicalName, LocalizedString caption) {
        super(canonicalName, caption, LM.getVersion());
        
        Version version = LM.getVersion();

        object = addSingleGroupObject(cls, version);

        ImList<PropertyClassImplement> idProps = LM.recognizeGroup.getProperties(cls, version);
        if(idProps.isEmpty()) {
            // we need at least one prop (otherwise there will be no grid in dialog)
            LCP objValueProp = LM.getObjValueProp(this, object);
            PropertyDrawEntity objectValue = addPropertyDraw(objValueProp, version, object);
            objectValue.setEditType(PropertyEditType.READONLY);
        }

        addPropertyDraw(object, version, LM.baseGroup);
    }

}
