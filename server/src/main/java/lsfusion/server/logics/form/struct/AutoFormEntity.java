package lsfusion.server.logics.form.struct;

import lsfusion.base.Pair;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.action.ServerResponse;
import lsfusion.server.base.version.Version;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.form.interactive.action.change.ActionObjectSelector;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.oraction.ActionOrPropertyObjectEntity;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

// should be added with addAutoFormEntity to be finalized
public abstract class AutoFormEntity extends FormEntity {

    public AutoFormEntity(LocalizedString caption, Version version) {
        super(null, null, caption, null, version);
    }

    public PropertyDrawEntity<?> addValuePropertyDraw(BaseLogicsModule LM, ObjectEntity object, Version version) {
        Pair<LP, ActionObjectSelector> valueProp = LM.getObjValueProp(this, object);
        PropertyDrawEntity propertyDraw = addPropertyDraw(valueProp.first, version, SetFact.singletonOrder(object));
        if(valueProp.second != null)
            propertyDraw.setEventAction(ServerResponse.CHANGE, valueProp.second, true);
        // assert that there is no richDesign / FormView yet
        propertyDraw.initCaption = LocalizedString.concatList(object.getCaption(), " (", LocalizedString.create("{logics.id}"), ")");
        return propertyDraw;
    }

    public <I extends PropertyInterface, P extends ActionOrProperty<I>> PropertyDrawEntity<I> addPropertyDraw(P property, ImRevMap<I, ObjectEntity> mapping, Version version) {
        ActionOrPropertyObjectEntity<I, ?> entity = ActionOrPropertyObjectEntity.create(property, mapping, null, null, null);
        return addPropertyDraw(entity, null, entity.property.getReflectionOrderInterfaces(), false, version);
    }
}
