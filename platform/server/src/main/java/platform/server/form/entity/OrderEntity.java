package platform.server.form.entity;

import platform.interop.serialization.CustomSerializable;
import platform.server.form.instance.Instantiable;
import platform.server.form.instance.OrderInstance;
import platform.server.form.instance.InstanceFactory;

import java.util.Set;

public interface OrderEntity<T extends OrderInstance> extends Instantiable<T>, CustomSerializable {
    void fillObjects(Set<ObjectEntity> objects);
}
