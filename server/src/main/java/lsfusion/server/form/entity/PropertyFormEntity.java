package lsfusion.server.form.entity;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.form.entity.filter.FilterEntity;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassType;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.group.AbstractGroup;

public class PropertyFormEntity extends FormEntity {

    public <P extends PropertyInterface, X extends PropertyInterface> PropertyFormEntity(BaseLogicsModule<? extends BusinessLogics<?>> LM, CalcProperty<P> property, CalcProperty<X> messageProperty, AbstractGroup recognizeGroup) {
        super(null, property.caption, LM.getVersion());

        Version version = LM.getVersion();
        
        if(messageProperty != null) {
            addPropertyDraw(messageProperty, MapFact.<X, ObjectEntity>EMPTYREV(), version);
        }

        ImMap<P,ValueClass> interfaceClasses = property.getInterfaceClasses(ClassType.logPolicy);
        boolean prev = property.usePrevHeur();
        
        ImRevMap<P, ObjectEntity> mapObjects = interfaceClasses.mapRevValues(new GetValue<ObjectEntity, ValueClass>() {
            public ObjectEntity getMapValue(ValueClass value) {
                // need to specify baseClass anyway, because we need it when adding recognizeGroup
                return new ObjectEntity(genID(), value, LocalizedString.create(value.toString(), false), true); // because heuristics can be incorrect, but we don't need classes (to be more specific, when there is DROPPED operator)
            }});
        
        GroupObjectEntity groupObject = new GroupObjectEntity(genID(), mapObjects.valuesSet().toOrderSet()); 
        addGroupObject(groupObject, version);

        // добавляем все свойства
        ImOrderSet<ObjectEntity> objects = groupObject.getOrderObjects();
        for(ObjectEntity object : objects)
            addPropertyDraw(LM.getObjValueProp(this, object), version, object);
        addPropertyDraw(recognizeGroup, prev, true, null, true, version, objects);

        //todo: раскомментить, чтобы можно было использовать форму в LogPropertyActionProperty
//        for (ObjectEntity object : objects) {
//            addPropertyDraw(LM.objectValue, false, object);
//        }

        addFixedFilter(new FilterEntity<>(new CalcPropertyObjectEntity<>(property, mapObjects)), version);

        finalizeInit(version);
    }
}
