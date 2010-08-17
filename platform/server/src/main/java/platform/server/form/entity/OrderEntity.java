package platform.server.form.entity;

import platform.server.form.instance.OrderInstance;
import platform.server.form.instance.InstanceFactory;

import java.util.Set;

public interface OrderEntity {

    OrderInstance getInstance(InstanceFactory instanceFactory);

    void fillObjects(Set<ObjectEntity> objects);
}
