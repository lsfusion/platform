package platform.server.form.entity;

import platform.server.form.instance.Instantiable;
import platform.server.form.instance.OrderInstance;
import platform.server.serialization.ServerCustomSerializable;

import java.util.Set;

public interface OrderEntity<T extends OrderInstance> extends Instantiable<T>, ServerCustomSerializable {
    void fillObjects(Set<ObjectEntity> objects);
}
