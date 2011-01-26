package platform.server.form.entity;

import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.Instantiable;
import platform.server.form.instance.OrderInstance;
import platform.server.serialization.ServerCustomSerializable;

import java.util.Set;

public interface OrderEntity<T extends OrderInstance> extends Instantiable<T>, ServerCustomSerializable {
    void fillObjects(Set<ObjectEntity> objects);

    /**
     * Возвращает OrderEntity, которая заменяет все ObjectEntity, на их текущие значения, взятые из instanceFactory,
     * кроме objectEntity, у которых baseClass равен baseClass'у object'a. В последнем случае возвращается сам object.
     *
     * По сути фиксирует текущие значения всех ObjectEntities, кроме тех, чей класс равен классу object
     */
    OrderEntity<T> getRemappedEntity(ObjectEntity object, InstanceFactory instanceFactory);
}
