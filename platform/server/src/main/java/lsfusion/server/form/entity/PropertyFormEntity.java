package lsfusion.server.form.entity;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.form.entity.filter.NotNullFilterEntity;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassType;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.group.AbstractGroup;

public class PropertyFormEntity<T extends BusinessLogics<T>> extends FormEntity<T> {

    public <P extends PropertyInterface> PropertyFormEntity(CalcProperty<P> property, AbstractGroup recognizeGroup) {
        super(null, null);

        ImMap<P,ValueClass> interfaceClasses = property.getInterfaceClasses(ClassType.ASSERTFULL);
        ImRevMap<P, ObjectEntity> mapObjects = interfaceClasses.mapRevValues(new GetValue<ObjectEntity, ValueClass>() {
            public ObjectEntity getMapValue(ValueClass value) {
                return new ObjectEntity(genID(), value, value.toString());
            }});

        GroupObjectEntity groupObject = new GroupObjectEntity(genID(), mapObjects.valuesSet().toOrderSet());
        addGroupObject(groupObject);

        // добавляем все свойства
        ImSet<ObjectEntity> objects = groupObject.getObjects();
        addPropertyDraw(recognizeGroup, true, true, objects.toList().toArray(new ObjectEntity[objects.size()]));

        addFixedFilter(new NotNullFilterEntity<P>(new CalcPropertyObjectEntity<P>(property, mapObjects)));
    }
}
