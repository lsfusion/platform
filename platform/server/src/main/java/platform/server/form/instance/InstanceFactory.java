package platform.server.form.instance;

import platform.server.form.entity.*;
import platform.server.form.entity.filter.*;
import platform.server.form.instance.filter.*;
import platform.server.logics.property.PropertyInterface;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class InstanceFactory {

    public InstanceFactory(PropertyObjectInterfaceInstance computer) {
        this.computer = computer;
    }

    private final Map<ObjectEntity, ObjectInstance> objectInstances = new HashMap<ObjectEntity, ObjectInstance>();
    private final Map<GroupObjectEntity, GroupObjectInstance> groupInstances = new HashMap<GroupObjectEntity, GroupObjectInstance>();
    private final Map<PropertyObjectEntity, PropertyObjectInstance> propertyObjectInstances = new HashMap<PropertyObjectEntity, PropertyObjectInstance>();
    private final Map<PropertyDrawEntity, PropertyDrawInstance> propertyDrawInstances = new HashMap<PropertyDrawEntity, PropertyDrawInstance>();


    public ObjectInstance getInstance(ObjectEntity entity) {
        if (!objectInstances.containsKey(entity)) {
            objectInstances.put(entity, entity.baseClass.newInstance(entity));
        }
        return objectInstances.get(entity);
    }

    public GroupObjectInstance getInstance(GroupObjectEntity entity) {

        if (entity == null) return null;

        if (!groupInstances.containsKey(entity)) {

            Collection<ObjectInstance> objects = new ArrayList<ObjectInstance>();
            for (ObjectEntity object : entity)
                objects.add(getInstance(object));

            groupInstances.put(entity, new GroupObjectInstance(entity, objects));
        }

        return groupInstances.get(entity);
    }

    public <P extends PropertyInterface> PropertyObjectInstance getInstance(PropertyObjectEntity<P> entity) {

        if (!propertyObjectInstances.containsKey(entity)) {
            Map<P, PropertyObjectInterfaceInstance> propertyMap = new HashMap<P, PropertyObjectInterfaceInstance>();
            for(Map.Entry<P, PropertyObjectInterfaceEntity> propertyImplement : entity.mapping.entrySet())
                propertyMap.put(propertyImplement.getKey(), propertyImplement.getValue().getInstance(this));
            propertyObjectInstances.put(entity, new PropertyObjectInstance<P>(entity.property, propertyMap));
        }

        return propertyObjectInstances.get(entity);
    }

    public <T extends PropertyInterface> PropertyDrawInstance getInstance(PropertyDrawEntity<T> entity) {
        GroupObjectEntity[] entColumnGroupObjects = entity.getColumnGroupObjects();
        PropertyDrawEntity[] entColumnDisplayProperties = entity.getColumnDisplayProperties();

        int length = entColumnGroupObjects == null ? 0 : entColumnGroupObjects.length;
        GroupObjectInstance columnGroupObjects[] = new GroupObjectInstance[length];
        PropertyDrawInstance columnDisplayProperties[] = new PropertyDrawInstance[length];
        for (int i = 0; i < length; ++i) {
            columnGroupObjects[i] = getInstance(entColumnGroupObjects[i]);
            columnDisplayProperties[i] = getInstance(entColumnDisplayProperties[i]);
        }

        if (!propertyDrawInstances.containsKey(entity)) {
            propertyDrawInstances.put(entity, new PropertyDrawInstance<T>(entity, getInstance(entity.propertyObject), getInstance(entity.toDraw), columnGroupObjects, columnDisplayProperties));
        }

        return propertyDrawInstances.get(entity);
    }

    public final PropertyObjectInterfaceInstance computer;
    public PropertyObjectInterfaceInstance getInstance(CurrentComputerEntity entity) {
        return computer;
    }

    public <P extends PropertyInterface> FilterInstance getInstance(CompareFilterEntity<P> entity) {
        return new CompareFilterInstance<P>(getInstance(entity.property), entity.compare, entity.value.getInstance(this));
    }

    public <P extends PropertyInterface> FilterInstance getInstance(IsClassFilterEntity<P> entity) {
        return new IsClassFilterInstance<P>(getInstance(entity.property), entity.isClass);
    }

    public <P extends PropertyInterface> FilterInstance getInstance(NotNullFilterEntity<P> entity) {
        return new NotNullFilterInstance<P>(getInstance(entity.property));
    }

    public FilterInstance getInstance(NotFilterEntity entity) {
        return new NotFilterInstance(entity.filter.getInstance(this));
    }

    public OrFilterInstance getInstance(OrFilterEntity entity) {
        return new OrFilterInstance(entity.op1.getInstance(this),entity.op2.getInstance(this));
    }

    public RegularFilterGroupInstance getInstance(RegularFilterGroupEntity entity) {

        RegularFilterGroupInstance group = new RegularFilterGroupInstance(entity);

        for (RegularFilterEntity filter : entity.filters) {
            group.addFilter(getInstance(filter));
        }

        return group;
    }

    public RegularFilterInstance getInstance(RegularFilterEntity entity) {
        return new RegularFilterInstance(entity, entity.filter.getInstance(this));
    }
}
