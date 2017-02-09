package lsfusion.server.form.entity;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.form.instance.InstanceFactory;
import lsfusion.server.form.instance.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;

public interface PropertyObjectInterfaceEntity extends OrderEntity<PropertyObjectInterfaceInstance> {
    PropertyObjectInterfaceEntity getRemappedEntity(ObjectEntity oldObject, ObjectEntity newObject, InstanceFactory instanceFactory);

    AndClassSet getAndClassSet();

    ObjectValue getObjectValue(ImMap<ObjectEntity, ObjectValue> mapObjects);
}
