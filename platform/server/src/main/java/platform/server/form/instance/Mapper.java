package platform.server.form.instance;

import platform.server.form.entity.*;
import platform.server.form.instance.listener.CustomClassListener;
import platform.server.logics.property.PropertyInterface;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Mapper {

    public final PropertyObjectInterfaceInstance computer;

    public Mapper(PropertyObjectInterfaceInstance computer) {
        this.computer = computer;
    }

    public final Map<ObjectEntity, ObjectInstance> objectMapper = new HashMap<ObjectEntity, ObjectInstance>();

    private ObjectInstance mapObject(ObjectEntity objKey, CustomClassListener classListener) {

        ObjectInstance objValue = objKey.baseClass.newObject(objKey.ID,objKey.getSID(),objKey.caption, classListener,objKey.addOnTransaction);

        objValue.resetOnApply = objKey.resetOnApply;

        objectMapper.put(objKey, objValue);
        return objValue;
    }

    public ObjectInstance mapObject(ObjectEntity entity) {
        return objectMapper.get(entity);
    }

    public final Map<GroupObjectEntity, GroupObjectInstance> groupMapper = new HashMap<GroupObjectEntity, GroupObjectInstance>();

    public GroupObjectInstance mapGroup(GroupObjectEntity groupKey,int order, CustomClassListener classListener) {

        Collection<ObjectInstance> objects = new ArrayList<ObjectInstance>();
        for (ObjectEntity object : groupKey)
            objects.add(mapObject(object, classListener));

        GroupObjectInstance groupValue = new GroupObjectInstance(groupKey.ID, objects, order,
                groupKey.pageSize, groupKey.initClassView, groupKey.banClassView);

        groupMapper.put(groupKey, groupValue);
        return groupValue;
    }

    private final Map<PropertyObjectEntity, PropertyObjectInstance> propertyMapper = new HashMap<PropertyObjectEntity, PropertyObjectInstance>();

    public <P extends PropertyInterface> PropertyObjectInstance mapProperty(PropertyObjectEntity<P> propKey) {

        if (propertyMapper.containsKey(propKey)) return propertyMapper.get(propKey);

        Map<P, PropertyObjectInterfaceInstance> propertyMap = new HashMap<P, PropertyObjectInterfaceInstance>();
        for(Map.Entry<P, PropertyObjectInterfaceEntity> propertyImplement : propKey.mapping.entrySet())
            propertyMap.put(propertyImplement.getKey(),propertyImplement.getValue().doMapping(this));
        PropertyObjectInstance propValue = new PropertyObjectInstance<P>(propKey.property, propertyMap);

        propertyMapper.put(propKey, propValue);
        return propValue;
    }

    private final Map<PropertyDrawEntity, PropertyDrawInstance> propertyDrawMapper = new HashMap<PropertyDrawEntity, PropertyDrawInstance>();

    public <T extends PropertyInterface> PropertyDrawInstance mapPropertyDraw(PropertyDrawEntity<T> propKey) {

        if (propertyDrawMapper.containsKey(propKey)) return propertyDrawMapper.get(propKey);

        PropertyDrawInstance propValue = new PropertyDrawInstance<T>(propKey.ID, propKey.getSID(), mapProperty(propKey.propertyObject), groupMapper.get(propKey.toDraw), propKey.forcePanel);
        
        propertyDrawMapper.put(propKey, propValue);
        return propValue;
    }
}
