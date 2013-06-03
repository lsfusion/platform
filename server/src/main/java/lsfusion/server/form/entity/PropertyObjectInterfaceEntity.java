package lsfusion.server.form.entity;

import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.form.instance.InstanceFactory;
import lsfusion.server.form.instance.PropertyObjectInterfaceInstance;

public interface PropertyObjectInterfaceEntity extends OrderEntity<PropertyObjectInterfaceInstance> {
    PropertyObjectInterfaceEntity getRemappedEntity(ObjectEntity oldObject, ObjectEntity newObject, InstanceFactory instanceFactory);

    AndClassSet getAndClassSet();
}
