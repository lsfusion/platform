package platform.server.form.entity;

import platform.server.form.instance.PropertyObjectInstance;
import platform.server.form.instance.Mapper;
import platform.server.logics.linear.LP;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyImplement;
import platform.server.logics.property.PropertyInterface;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class PropertyObjectEntity<P extends PropertyInterface> extends PropertyImplement<PropertyObjectInterfaceEntity,P> implements OrderEntity {

    public Collection<ObjectEntity> getObjectInstances() {
        Collection<ObjectEntity> result = new ArrayList<ObjectEntity>();
        for(PropertyObjectInterfaceEntity object : mapping.values())
            if(object instanceof ObjectEntity)
                result.add((ObjectEntity) object);
        return result;
    }

    public void fillObjects(Set<ObjectEntity> objects) {
        objects.addAll(getObjectInstances());
    }

    public PropertyObjectEntity(LP<P> property, PropertyObjectInterfaceEntity... objects) {
        super(property.property);

        for(int i=0;i<property.listInterfaces.size();i++)
            mapping.put(property.listInterfaces.get(i),objects[i]);
    }

    public PropertyObjectEntity(Property<P> property, Map<P, PropertyObjectInterfaceEntity> mapping) {
        super(property, mapping);
    }

    public PropertyObjectInstance doMapping(Mapper mapper) {
        return mapper.mapProperty(this);
    }
}
