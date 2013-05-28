package platform.server.form.entity;

import platform.server.classes.sets.AndClassSet;
import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.PropertyObjectInterfaceInstance;

public interface PropertyObjectInterfaceEntity extends OrderEntity<PropertyObjectInterfaceInstance> {
    PropertyObjectInterfaceEntity getRemappedEntity(ObjectEntity oldObject, ObjectEntity newObject, InstanceFactory instanceFactory);

    AndClassSet getAndClassSet();
}
