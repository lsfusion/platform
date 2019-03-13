package lsfusion.server.logics.form.struct.property;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.logics.form.interactive.InstanceFactory;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.order.OrderEntity;

public interface PropertyObjectInterfaceEntity extends OrderEntity<PropertyObjectInterfaceInstance> {
    PropertyObjectInterfaceInstance getRemappedInstance(ObjectEntity oldObject, ObjectInstance newObject, InstanceFactory instanceFactory);

    AndClassSet getAndClassSet();

    ObjectValue getObjectValue(ImMap<ObjectEntity, ? extends ObjectValue> mapObjects);
}
