package platform.server.form.entity;

import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.Mapper;

public interface PropertyObjectInterfaceEntity extends OrderEntity {

    PropertyObjectInterfaceInstance doMapping(Mapper mapper);
}
