package platform.server.form.entity;

import platform.server.form.instance.OrderInstance;
import platform.server.form.instance.Mapper;
import platform.server.form.entity.ObjectEntity;

import java.util.Set;

public interface OrderEntity {

    OrderInstance doMapping(Mapper mapper);

    void fillObjects(Set<ObjectEntity> objects);
}
