package lsfusion.server.form.entity;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.form.entity.filter.NotNullFilterEntity;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.group.AbstractGroup;

public class PropertyFormEntity<T extends BusinessLogics<T>> extends FormEntity<T> {

    public <P extends PropertyInterface> PropertyFormEntity(BaseLogicsModule<? extends BusinessLogics<?>> LM, CalcProperty<P> property, AbstractGroup recognizeGroup) {
        super(null, null, LM.getVersion());

        Version version = LM.getVersion();

        ImMap<P,ValueClass> interfaceClasses = property.getInterfaceClasses(ClassType.ASSERTFULL);
        boolean prev = false;
        if(interfaceClasses.isEmpty()) {
            interfaceClasses = property.getInterfaceClasses(ClassType.ASSERTFULL, PrevClasses.INVERTSAME);
            prev = true;
        }
        ImRevMap<P, ObjectEntity> mapObjects = interfaceClasses.mapRevValues(new GetValue<ObjectEntity, ValueClass>() {
            public ObjectEntity getMapValue(ValueClass value) {
                return new ObjectEntity(genID(), value, value.toString());
            }});

        GroupObjectEntity groupObject = new GroupObjectEntity(genID(), mapObjects.valuesSet().toOrderSet(), prev);
        addGroupObject(groupObject, version);

        // добавляем все свойства
        ImSet<ObjectEntity> objects = groupObject.getObjects();
        addPropertyDraw(recognizeGroup, prev, true, true, version, objects.toList().toArray(new ObjectEntity[objects.size()]));

        //todo: раскомментить, чтобы можно было использовать форму в LogPropertyActionProperty
//        for (ObjectEntity object : objects) {
//            addPropertyDraw(LM.objectValue, false, object);
//        }

        addFixedFilter(new NotNullFilterEntity<P>(new CalcPropertyObjectEntity<P>(property, mapObjects)), version);

        finalizeInit(version);
    }
}
