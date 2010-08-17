package platform.server.form.entity;

import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.PropertyObjectInterfaceInstance;

public interface PropertyObjectInterfaceEntity extends OrderEntity {

    PropertyObjectInterfaceInstance getInstance(InstanceFactory instanceFactory);
}
