package lsfusion.server.logics.form.struct;

import lsfusion.base.Pair;
import lsfusion.interop.action.ServerResponse;
import lsfusion.server.base.version.Version;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.form.interactive.action.change.ActionObjectSelector;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

// should be added with addAutoFormEntity to be finalized
public abstract class AutoFormEntity extends FormEntity {

    public AutoFormEntity(LocalizedString caption, Version version) {
        super(null, null, caption, null, version);
    }

    public PropertyDrawEntity<?> addValuePropertyDraw(LogicsModule LM, ObjectEntity object, Version version) {
        Pair<LP, ActionObjectSelector> valueProp = LM.getObjValueProp(this, object);
        PropertyDrawEntity propertyDraw = addPropertyDraw(valueProp.first, version, object);
        if(valueProp.second != null)
            propertyDraw.setEventAction(ServerResponse.CHANGE, valueProp.second);
        return propertyDraw;
    }
}
