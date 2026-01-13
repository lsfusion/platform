package lsfusion.server.logics.form.struct;

import lsfusion.base.Pair;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.base.version.Version;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.oraction.ActionOrPropertyObjectEntity;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.util.List;

// should be added with addAutoFormEntity to be finalized
public abstract class AutoFormEntity extends FormEntity {

    public AutoFormEntity(boolean needDesign, Version version) {
        super(needDesign, null, version, null);
    }

    public PropertyDrawEntity<?, ?> addValuePropertyDraw(BaseLogicsModule LM, ObjectEntity object, Version version) {
        LP valueProp = LM.getObjValueProp(this, object);
        PropertyDrawEntity propertyDraw = addPropertyDraw(valueProp, version, SetFact.singletonOrder(object));

        FormView view = this.view;
        if(view != null) { // if !needDesign there is no view
            // it's better to do as getDefaultCaption, but AutoFormEntity is not extensible anyway
            propertyDraw.setCaption(LocalizedString.concatList(object.getNFCaption(version), " (", LocalizedString.create("{logics.id}"), ")"), version);
        }
        return propertyDraw;
    }

    public <I extends PropertyInterface, P extends ActionOrProperty<I>> PropertyDrawEntity<I, ?> addPropertyDraw(P property, ImRevMap<I, ObjectEntity> mapping, Version version) {
        return addPropertyDraw(property, null, property.getReflectionOrderInterfaces(), mapping, version);
    }

    public <I extends PropertyInterface, P extends ActionOrProperty<I>> PropertyDrawEntity<I, ?> addPropertyDraw(P property, Pair<ActionOrProperty, List<String>> inherited, ImOrderSet<I> orderInterfaces, ImRevMap<I, ObjectEntity> mapping, Version version) {
        ActionOrPropertyObjectEntity<I, ?, ?> entity = ActionOrPropertyObjectEntity.create(property, mapping, null, null, null);
        return addPropertyDraw(entity, inherited, orderInterfaces, version);
    }

    protected GroupObjectEntity addGroupObjectEntity(BaseLogicsModule LM, ImOrderSet<ObjectEntity> objects, Version version) {
        for(ObjectEntity object : objects)
            addObject(object, version);

        GroupObjectEntity groupObject = new GroupObjectEntity(genID, null, objects, LM, null);
        addGroupObject(groupObject, version);
        return groupObject;
    }
}
