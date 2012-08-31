package platform.server.form.entity;

import platform.server.classes.ValueClass;
import platform.server.form.entity.filter.NotNullFilterEntity;
import platform.server.logics.BusinessLogics;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.group.AbstractGroup;

import java.util.HashMap;
import java.util.Map;

public class PropertyFormEntity<T extends BusinessLogics<T>> extends FormEntity<T> {

    public <P extends PropertyInterface> PropertyFormEntity(CalcProperty<P> property, AbstractGroup recognizeGroup) {
        super(null, null);

        GroupObjectEntity groupObject = new GroupObjectEntity(genID());
        Map<P, ObjectEntity> mapObjects = new HashMap<P, ObjectEntity>();
        for(Map.Entry<P, ValueClass> propInterface : property.getInterfaceClasses(true).entrySet()) {
            ObjectEntity objectEntity = new ObjectEntity(genID(), propInterface.getValue(), propInterface.toString());
            groupObject.add(objectEntity);
            mapObjects.put(propInterface.getKey(), objectEntity);
        }
        addGroupObject(groupObject);

        // добавляем все свойства
        addPropertyDraw(recognizeGroup, true, true, groupObject.objects.toArray(new ObjectEntity[groupObject.objects.size()]));

        addFixedFilter(new NotNullFilterEntity<P>(new CalcPropertyObjectEntity<P>(property, mapObjects)));
    }
}
